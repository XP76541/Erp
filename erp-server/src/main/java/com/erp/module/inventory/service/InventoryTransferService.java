package com.erp.module.inventory.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.inventory.entity.InventoryTransfer;
import com.erp.module.inventory.entity.InventoryTransferItem;
import com.erp.module.inventory.mapper.InventoryTransferMapper;
import com.erp.module.inventory.mapper.InventoryTransferItemMapper;
import com.erp.module.inventory.dto.InventoryTransferDtos;
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
 * 库存调拨单服务
 * 审核 = 单一大事务:抢占状态机 → 出库调拨 + 入库调拨 → 操作日志
 * 任一步失败整体回滚
 */
@Service
public class InventoryTransferService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InventoryTransferMapper transferMapper;
    private final InventoryTransferItemMapper itemMapper;
    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    public InventoryTransferService(InventoryTransferMapper transferMapper,
                                   InventoryTransferItemMapper itemMapper,
                                   WarehouseMapper warehouseMapper,
                                   ProductMapper productMapper,
                                   InventoryService inventoryService,
                                   DocSequenceService docSequenceService,
                                   OperationLogService operationLogService) {
        this.transferMapper = transferMapper;
        this.itemMapper = itemMapper;
        this.warehouseMapper = warehouseMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
    }

    /** 分页列表:按单号/状态/仓库过滤 */
    public PageResult<InventoryTransferDtos.ListResponse> page(long page, long size,
            String keyword, String status, Long warehouseId) {
        Page<InventoryTransfer> result = transferMapper.selectPage(new Page<>(page, size),
                Wrappers.<InventoryTransfer>lambdaQuery()
                        .like(StringUtils.hasText(keyword), InventoryTransfer::getDocNo, keyword)
                        .eq(StringUtils.hasText(status), InventoryTransfer::getStatus, status)
                        .eq(warehouseId != null, InventoryTransfer::getFromWarehouseId, warehouseId)
                        .orderByDesc(InventoryTransfer::getId));

        List<InventoryTransferDtos.ListResponse> responses = result.getRecords().stream()
                .map(InventoryTransferDtos.ListResponse::new)
                .toList();

        return PageResult.of(result.getTotal(), responses);
    }

    /** 单据详情(主表+明细) */
    public InventoryTransferDtos.DetailResponse detail(Long id) {
        InventoryTransfer doc = requireDoc(id);
        List<InventoryTransferItem> items = itemMapper.selectByTransferId(id);
        List<InventoryTransferDtos.ItemDetail> itemDetails = items.stream()
                .map(InventoryTransferDtos.ItemDetail::new)
                .toList();
        return new InventoryTransferDtos.DetailResponse(doc, itemDetails);
    }

    /** 创建草稿:校验档案引用与状态,服务端计算金额 */
    @Transactional
    public Long create(InventoryTransferDtos.CreateRequest request, TokenStore.LoginUser user) {
        // 验证仓库
        Warehouse fromWarehouse = warehouseMapper.selectById(request.getFromWarehouseId());
        if (fromWarehouse == null || fromWarehouse.getIsActive() == 0) {
            throw new BusinessException("调出仓库不存在或已停用");
        }
        Warehouse toWarehouse = warehouseMapper.selectById(request.getToWarehouseId());
        if (toWarehouse == null || toWarehouse.getIsActive() == 0) {
            throw new BusinessException("调入仓库不存在或已停用");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("明细不能为空");
        }
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new BusinessException("调出仓库和调入仓库不能相同");
        }

        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();

        // 创建调拨单
        InventoryTransfer doc = new InventoryTransfer();
        doc.setDocNo(docSequenceService.nextDocNo("TF", "TF", bizDate.format(PERIOD)));
        doc.setFromWarehouseId(request.getFromWarehouseId());
        doc.setToWarehouseId(request.getToWarehouseId());
        doc.setBizDate(bizDate);
        doc.setStatus("DRAFT");
        doc.setRemark(request.getRemark() == null ? "" : request.getRemark());
        doc.setOperatorId(user.userId());
        transferMapper.insert(doc);

        // 创建调拨明细
        int lineNo = 0;
        for (InventoryTransferDtos.ItemInput input : request.getItems()) {
            lineNo++;
            Product product = productMapper.selectById(input.getProductId());
            if (product == null || product.getIsActive() == 0) {
                throw new BusinessException("第 " + lineNo + " 行商品不存在或已停用");
            }

            // 检查调出仓库是否有足够库存
            BigDecimal currentStock = inventoryService.getStockQuantity(
                    input.getProductId(), request.getFromWarehouseId());
            if (currentStock.compareTo(input.getQty()) < 0) {
                throw new BusinessException("第 " + lineNo + " 行商品在调出仓库库存不足");
            }

            InventoryTransferItem item = new InventoryTransferItem();
            item.setTransferId(doc.getId());
            item.setLineNo(lineNo);
            item.setProductId(input.getProductId());
            item.setFromWarehouseId(request.getFromWarehouseId());
            item.setToWarehouseId(request.getToWarehouseId());
            item.setQty(input.getQty());
            item.setPrice(input.getPrice() != null ? input.getPrice() : BigDecimal.ZERO);
            item.setAmount(input.getQty().multiply(item.getPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setNote(input.getNote() == null ? "" : input.getNote());
            itemMapper.insert(item);
        }

        return doc.getId();
    }

    /**
     * 审核 = 单一大事务:抢占状态机 → 出库调拨 + 入库调拨 → 操作日志
     */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击审核只有一次生效
        if (transferMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        InventoryTransfer doc = requireDoc(id);

        // ② 出库调拨:从调出仓库减少库存
        List<InventoryTransferItem> items = itemMapper.selectByTransferId(id);
        for (InventoryTransferItem item : items) {
            inventoryService.stockOut("TRANSFER_OUT", doc.getId(), doc.getDocNo(),
                    item.getProductId(), item.getFromWarehouseId(), item.getQty(), doc.getBizDate());
        }

        // ③ 入库调拨:向调入仓库增加库存
        for (InventoryTransferItem item : items) {
            inventoryService.stockIn("TRANSFER_IN", doc.getId(), doc.getDocNo(),
                    item.getProductId(), item.getToWarehouseId(), item.getQty(), item.getPrice(),
                    doc.getBizDate());
        }

        // ④ 操作日志(审计留痕)
        String detail = "{\"amount\":" + totalAmount(items) + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "inventory_transfer", "AUDIT",
                "TRANSFER", id, doc.getDocNo(), detail, ip);
    }

    /**
     * 完成调拨
     */
    @Transactional
    public void complete(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发操作只有一次生效
        if (transferMapper.claimComplete(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是已审核状态,无法完成");
        }
        InventoryTransfer doc = requireDoc(id);

        // ② 记录完成日志
        operationLogService.record(user, "inventory_transfer", "COMPLETE",
                "TRANSFER", id, doc.getDocNo(), "{\"total_amount\":" + doc.getTotalAmount() + "}", ip);
    }

    /**
     * 取消调拨
     */
    @Transactional
    public void cancel(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发操作只有一次生效
        if (transferMapper.claimCancel(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法取消");
        }
        InventoryTransfer doc = requireDoc(id);

        // ② 记录取消日志
        operationLogService.record(user, "inventory_transfer", "CANCEL",
                "TRANSFER", id, doc.getDocNo(), "{\"reason\":\"用户取消\"}", ip);
    }

    /** 根据仓库ID查询调拨列表 */
    public List<InventoryTransferDtos.WarehouseResponse> listByWarehouse(Long warehouseId) {
        List<InventoryTransfer> transfers = transferMapper.selectByWarehouseId(warehouseId);
        return transfers.stream()
                .map(InventoryTransferDtos.WarehouseResponse::new)
                .toList();
    }

    /** 调拨统计 */
    public InventoryTransferDtos.StatsResponse getStats() {
        Integer draftCount = transferMapper.selectCount(
                Wrappers.<InventoryTransfer>lambdaQuery()
                        .eq(InventoryTransfer::getStatus, "DRAFT")).intValue();
        Integer auditCount = transferMapper.selectCount(
                Wrappers.<InventoryTransfer>lambdaQuery()
                        .eq(InventoryTransfer::getStatus, "AUDITED")).intValue();
        Integer completedCount = transferMapper.selectCount(
                Wrappers.<InventoryTransfer>lambdaQuery()
                        .eq(InventoryTransfer::getStatus, "COMPLETED")).intValue();
        BigDecimal totalAmount = transferMapper.selectList(Wrappers.emptyList()).stream()
                .map(InventoryTransfer::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryTransferDtos.StatsResponse(draftCount, auditCount, completedCount, totalAmount);
    }

    private BigDecimal totalAmount(List<InventoryTransferItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (InventoryTransferItem item : items) {
            total = total.add(item.getAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private InventoryTransfer requireDoc(Long id) {
        InventoryTransfer doc = transferMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("库存调拨单不存在");
        }
        return doc;
    }
}