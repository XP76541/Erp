package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.inventory.entity.InventoryCheck;
import com.erp.module.inventory.entity.InventoryCheckItem;
import com.erp.module.inventory.mapper.InventoryCheckMapper;
import com.erp.module.inventory.mapper.InventoryCheckItemMapper;
import com.erp.module.inventory.dto.InventoryCheckDtos;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 库存盘点单服务
 * 盘点 = 单一大事务:抢占状态机 → 记录盘点数量 → 计算差异 → 操作日志
 * 任一步失败整体回滚
 */
@Service
public class InventoryCheckService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InventoryCheckMapper checkMapper;
    private final InventoryCheckItemMapper itemMapper;
    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    public InventoryCheckService(InventoryCheckMapper checkMapper,
                                 InventoryCheckItemMapper itemMapper,
                                 WarehouseMapper warehouseMapper,
                                 ProductMapper productMapper,
                                 InventoryService inventoryService,
                                 DocSequenceService docSequenceService,
                                 OperationLogService operationLogService) {
        this.checkMapper = checkMapper;
        this.itemMapper = itemMapper;
        this.warehouseMapper = warehouseMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
    }

    /** 分页列表:按单号/状态/仓库过滤 */
    public PageResult<InventoryCheckDtos.ListResponse> page(long page, long size,
            String keyword, String status, Long warehouseId) {
        Page<InventoryCheck> result = checkMapper.selectPage(new Page<>(page, size),
                Wrappers.<InventoryCheck>lambdaQuery()
                        .like(StringUtils.hasText(keyword), InventoryCheck::getDocNo, keyword)
                        .eq(StringUtils.hasText(status), InventoryCheck::getStatus, status)
                        .eq(warehouseId != null, InventoryCheck::getWarehouseId, warehouseId)
                        .orderByDesc(InventoryCheck::getId));

        List<InventoryCheckDtos.ListResponse> responses = result.getRecords().stream()
                .map(InventoryCheckDtos.ListResponse::new)
                .toList();

        return PageResult.of(result.getTotal(), responses);
    }

    /** 单据详情(主表+明细) */
    public InventoryCheckDtos.DetailResponse detail(Long id) {
        InventoryCheck doc = requireDoc(id);
        List<InventoryCheckItem> items = itemMapper.selectByCheckId(id);
        List<InventoryCheckDtos.ItemDetail> itemDetails = items.stream()
                .map(InventoryCheckDtos.ItemDetail::new)
                .toList();
        return new InventoryCheckDtos.DetailResponse(doc, itemDetails);
    }

    /** 创建草稿:校验档案引用 */
    @Transactional
    public Long create(InventoryCheckDtos.CreateRequest request, TokenStore.LoginUser user) {
        // 验证仓库
        Warehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || warehouse.getIsActive() == 0) {
            throw new BusinessException("仓库不存在或已停用");
        }

        LocalDate checkDate = request.getCheckDate() == null ? LocalDate.now() : request.getCheckDate();
        String checkType = request.getCheckType() == null ? "FULL" : request.getCheckType();

        // 检查当天是否已有盘点单
        List<InventoryCheck> existingChecks = checkMapper.selectByWarehouseAndDate(
                request.getWarehouseId(), checkDate);
        if (!existingChecks.isEmpty()) {
            throw new BusinessException("该仓库在选定日期已有盘点单");
        }

        // 创建盘点单
        InventoryCheck doc = new InventoryCheck();
        doc.setDocNo(docSequenceService.nextDocNo("CHK", "CHK", checkDate.format(PERIOD)));
        doc.setWarehouseId(request.getWarehouseId());
        doc.setCheckDate(checkDate);
        doc.setStatus("DRAFT");
        doc.setCheckType(checkType);
        doc.setOperatorId(user.userId());
        doc.setRemark(request.getRemark() == null ? "" : request.getRemark());
        checkMapper.insert(doc);

        // 创建盘点明细
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            int lineNo = 0;
            for (InventoryCheckDtos.ItemInput input : request.getItems()) {
                lineNo++;
                Product product = productMapper.selectById(input.getProductId());
                if (product == null || product.getIsActive() == 0) {
                    throw new BusinessException("第 " + lineNo + " 行商品不存在或已停用");
                }

                // 获取系统库存数量
                BigDecimal systemQty = inventoryService.getStockQuantity(
                        input.getProductId(), request.getWarehouseId());

                InventoryCheckItem item = new InventoryCheckItem();
                item.setCheckId(doc.getId());
                item.setProductId(input.getProductId());
                item.setWarehouseId(request.getWarehouseId());
                item.setSystemQty(systemQty);
                item.setActualQty(BigDecimal.ZERO); // 初始为0，盘点时录入
                item.setDiffQty(BigDecimal.ZERO);
                item.setPrice(input.getPrice() != null ? input.getPrice() : BigDecimal.ZERO);
                item.setAmount(BigDecimal.ZERO);
                item.setStatus("NORMAL");
                item.setNote(input.getNote() == null ? "" : input.getNote());
                itemMapper.insert(item);
            }
        }

        return doc.getId();
    }

    /** 开始盘点:抢占状态机 */
    @Transactional
    public void startCheck(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发操作只有一次生效
        if (checkMapper.claimCheck(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法开始盘点");
        }
        InventoryCheck doc = requireDoc(id);

        // ② 记录开始盘点日志
        operationLogService.record(user, "inventory_check", "START_CHECK",
                "CHECK", id, doc.getDocNo(), "{\"check_type\":\"" + doc.getCheckType() + "\"}", ip);
    }

    /** 提交盘点结果:更新实际数量和差异 */
    @Transactional
    public void submitResult(Long id, InventoryCheckDtos.SubmitResultRequest request, TokenStore.LoginUser user, String ip) {
        InventoryCheck doc = requireDoc(id);
        if (!"AUDITING".equals(doc.getStatus())) {
            throw new BusinessException("单据不是盘点状态,无法提交结果");
        }

        // 更新盘点明细的实际数量
        for (InventoryCheckDtos.ItemResult result : request.getItems()) {
            InventoryCheckItem item = itemMapper.selectOne(
                    Wrappers.<InventoryCheckItem>lambdaQuery()
                            .eq(InventoryCheckItem::getCheckId, id)
                            .eq(InventoryCheckItem::getProductId, result.getProductId()));

            if (item != null) {
                item.setActualQty(result.getActualQty());
                item.setDiffQty(result.getActualQty().subtract(item.getSystemQty()));

                // 计算金额
                BigDecimal price = productMapper.selectById(result.getProductId()).getPrice();
                item.setPrice(price);
                item.setAmount(price.multiply(result.getActualQty()).setScale(2, RoundingMode.HALF_UP));

                // 判断状态
                if (item.getDiffQty().compareTo(BigDecimal.ZERO) == 0) {
                    item.setStatus("NORMAL");
                } else if (item.getDiffQty().compareTo(BigDecimal.ZERO) < 0) {
                    item.setStatus("MISSING");
                } else {
                    item.setStatus("EXCESS");
                }

                item.setNote(result.getNote() == null ? "" : result.getNote());
                itemMapper.updateById(item);
            }
        }

        // 更新盘点单统计
        updateCheckStats(id);

        // 记录提交结果日志
        operationLogService.record(user, "inventory_check", "SUBMIT_RESULT",
                "CHECK", id, doc.getDocNo(), "{\"items\":" + request.getItems().size() + "}", ip);
    }

    /** 审核 = 单一大事务:抢占状态机 → 记录审核 → 操作日志 */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发操作只有一次生效
        if (checkMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是盘点中状态,无法审核");
        }
        InventoryCheck doc = requireDoc(id);

        // ② 更新盘点单状态
        doc.setStatus("AUDITED");
        doc.setAuditBy(user.userId());
        doc.setAuditAt(java.time.LocalDateTime.now());
        checkMapper.updateById(doc);

        // ③ 记录审核日志
        operationLogService.record(user, "inventory_check", "AUDIT",
                "CHECK", id, doc.getDocNo(), "{\"diff_items\":" + doc.getDiffItems() + ",\"diff_amount\":" + doc.getDiffAmount() + "}", ip);
    }

    /** 取消盘点单 */
    @Transactional
    public void cancel(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发操作只有一次生效
        if (checkMapper.claimCancel(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法取消");
        }
        InventoryCheck doc = requireDoc(id);

        // ② 记录取消日志
        operationLogService.record(user, "inventory_check", "CANCEL",
                "CHECK", id, doc.getDocNo(), "{\"reason\":\"用户取消\"}", ip);
    }

    /** 根据仓库ID查询盘点列表 */
    public List<InventoryCheckDtos.WarehouseResponse> listByWarehouse(Long warehouseId) {
        List<InventoryCheck> checks = checkMapper.selectByWarehouseId(warehouseId);
        return checks.stream()
                .map(InventoryCheckDtos.WarehouseResponse::new)
                .toList();
    }

    /** 盘点统计 */
    public InventoryCheckDtos.StatsResponse getStats() {
        Integer draftCount = checkMapper.selectCount(
                Wrappers.<InventoryCheck>lambdaQuery()
                        .eq(InventoryCheck::getStatus, "DRAFT")).intValue();
        Integer checkingCount = checkMapper.selectCount(
                Wrappers.<InventoryCheck>lambdaQuery()
                        .eq(InventoryCheck::getStatus, "AUDITING")).intValue();
        Integer auditedCount = checkMapper.selectCount(
                Wrappers.<InventoryCheck>lambdaQuery()
                        .eq(InventoryCheck::getStatus, "AUDITED")).intValue();

        // 查询总金额和差异数据
        List<InventoryCheck> allChecks = checkMapper.selectList(Wrappers.emptyWrapper());
        BigDecimal totalAmount = allChecks.stream()
                .map(InventoryCheck::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diffAmount = allChecks.stream()
                .map(InventoryCheck::getDiffAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryCheckDtos.StatsResponse(draftCount, checkingCount, auditedCount, totalAmount, diffAmount);
    }

    /** 更新盘点单统计信息 */
    private void updateCheckStats(Long checkId) {
        InventoryCheck check = requireDoc(checkId);

        // 重新计算统计信息
        List<InventoryCheckItem> items = itemMapper.selectByCheckId(checkId);

        Integer totalItems = items.size();
        BigDecimal totalAmount = items.stream()
                .map(InventoryCheckItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer diffItems = (int) items.stream()
                .filter(item -> !"NORMAL".equals(item.getStatus()))
                .count();

        BigDecimal diffAmount = items.stream()
                .filter(item -> !"NORMAL".equals(item.getStatus()))
                .map(InventoryCheckItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        checkMapper.updateCheckStats(checkId, totalItems, totalAmount, diffItems, diffAmount);
    }

    private InventoryCheck requireDoc(Long id) {
        InventoryCheck doc = checkMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("库存盘点单不存在");
        }
        return doc;
    }
}