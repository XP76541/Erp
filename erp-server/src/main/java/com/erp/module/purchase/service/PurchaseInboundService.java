package com.erp.module.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.SupplierMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.inventory.service.InventoryService;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.purchase.entity.PurchaseInbound;
import com.erp.module.purchase.entity.PurchaseInboundItem;
import com.erp.module.purchase.mapper.PurchaseInboundMapper;
import com.erp.module.purchase.mapper.PurchaseInboundItemMapper;
import com.erp.module.purchase.dto.PurchaseInboundDtos;
import com.erp.module.purchase.dto.PurchaseInboundDtos.CreateRequest;
import com.erp.module.purchase.dto.PurchaseInboundDtos.ItemInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 采购入库单(F201/US-201):草稿创建 + 审核
 * 审核 = 单一大事务:抢占状态机 → 逐行入库(行锁+加权平均+台账) → 生成应付 → 操作日志
 * 任一步失败整体回滚,见 docs/database-design.md §6.2
 */
@Service
public class PurchaseInboundService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseInboundMapper inboundMapper;
    private final PurchaseInboundItemMapper itemMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final PayableMapper payableMapper;

    public PurchaseInboundService(PurchaseInboundMapper inboundMapper,
                                  PurchaseInboundItemMapper itemMapper,
                                  SupplierMapper supplierMapper,
                                  WarehouseMapper warehouseMapper,
                                  ProductMapper productMapper,
                                  InventoryService inventoryService,
                                  DocSequenceService docSequenceService,
                                  OperationLogService operationLogService,
                                  PayableMapper payableMapper) {
        this.inboundMapper = inboundMapper;
        this.itemMapper = itemMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.productMapper = productMapper;
        this.inventoryService = inventoryService;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
        this.payableMapper = payableMapper;
    }

    /** 分页列表:按单号/状态过滤 */
    public PageResult<PurchaseInbound> page(long page, long size, String keyword, String status) {
        Page<PurchaseInbound> result = inboundMapper.selectPage(new Page<>(page, size),
                Wrappers.<PurchaseInbound>lambdaQuery()
                        .like(StringUtils.hasText(keyword), PurchaseInbound::getDocNo, keyword)
                        .eq(StringUtils.hasText(status), PurchaseInbound::getStatus, status)
                        .orderByDesc(PurchaseInbound::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    /** 单据详情(主表+明细) */
    public PurchaseInboundDtos.DetailResponse detail(Long id) {
        PurchaseInbound doc = requireDoc(id);
        List<PurchaseInboundItem> items = itemMapper.selectList(
                Wrappers.<PurchaseInboundItem>lambdaQuery()
                        .eq(PurchaseInboundItem::getInboundId, id)
                        .orderByAsc(PurchaseInboundItem::getLineNo));
        return new PurchaseInboundDtos.DetailResponse(doc, items);
    }

    /** 创建草稿:校验档案引用与状态,服务端计算金额 */
    @Transactional
    public Long create(CreateRequest request, TokenStore.LoginUser user) {
        Supplier supplier = supplierMapper.selectById(request.getSupplierId());
        if (supplier == null || supplier.getIsActive() == 0) {
            throw new BusinessException("供应商不存在或已停用");
        }
        Warehouse warehouse = warehouseMapper.selectById(request.getWarehouseId());
        if (warehouse == null || warehouse.getIsActive() == 0) {
            throw new BusinessException("仓库不存在或已停用");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("明细不能为空");
        }
        LocalDate bizDate = request.getBizDate() == null ? LocalDate.now() : request.getBizDate();
        Set<Long> productIds = new HashSet<>();

        PurchaseInbound doc = new PurchaseInbound();
        doc.setDocNo(docSequenceService.nextDocNo("PIN", "PIN", bizDate.format(PERIOD)));
        doc.setSupplierId(request.getSupplierId());
        doc.setDocType(request.getDocType());
        doc.setDocId(request.getDocId());
        doc.setWarehouseId(request.getWarehouseId());
        doc.setBizDate(bizDate);
        doc.setStatus("DRAFT");
        doc.setRemark(request.getRemark() == null ? "" : request.getRemark());
        doc.setCreatedBy(user.userId());
        inboundMapper.insert(doc);

        int lineNo = 0;
        for (ItemInput input : request.getItems()) {
            lineNo++;
            Product product = productMapper.selectById(input.getProductId());
            if (product == null || product.getIsActive() == 0) {
                throw new BusinessException("第 " + lineNo + " 行商品不存在或已停用");
            }
            if (!productIds.add(input.getProductId())) {
                throw new BusinessException("第 " + lineNo + " 行商品重复,请合并数量");
            }
            if (input.getQty() == null || input.getQty().signum() <= 0
                    || input.getQty().scale() > 4) {
                throw new BusinessException("第 " + lineNo + " 行数量必须大于0且最多4位小数");
            }
            if (input.getPrice() == null || input.getPrice().signum() < 0
                    || input.getPrice().scale() > 2) {
                throw new BusinessException("第 " + lineNo + " 行进价不能为负且最多2位小数");
            }
            Long lineWarehouseId = input.getWarehouseId() == null
                    ? request.getWarehouseId() : input.getWarehouseId();
            Warehouse lineWarehouse = warehouseMapper.selectById(lineWarehouseId);
            if (lineWarehouse == null || lineWarehouse.getIsActive() == 0) {
                throw new BusinessException("第 " + lineNo + " 行仓库不存在或已停用");
            }
            PurchaseInboundItem item = new PurchaseInboundItem();
            item.setInboundId(doc.getId());
            item.setLineNo(lineNo);
            item.setProductId(input.getProductId());
            item.setWarehouseId(lineWarehouseId);
            item.setQty(input.getQty());
            item.setPrice(input.getPrice());
            item.setAmount(input.getQty().multiply(input.getPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setNote(input.getNote() == null ? "" : input.getNote());
            itemMapper.insert(item);
        }
        return doc.getId();
    }

    /**
     * 审核 = 单一大事务(§6.2 红线):
     * ① 原子抢占 DRAFT→AUDITED ② 逐行入库(行锁+加权平均+台账) ③ 生成应付 ④ 审计字段 ⑤ 操作日志
     */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击审核只有一次生效,失败者读到已审状态报错回滚
        if (inboundMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        PurchaseInbound doc = requireDoc(id);

        // ② 逐行入库:库存行锁 + 加权平均 + 台账快照
        List<PurchaseInboundItem> items = itemMapper.selectList(
                Wrappers.<PurchaseInboundItem>lambdaQuery()
                        .eq(PurchaseInboundItem::getInboundId, id)
                        .orderByAsc(PurchaseInboundItem::getLineNo));
        for (PurchaseInboundItem item : items) {
            inventoryService.stockIn("PURCHASE_IN", doc.getId(), doc.getDocNo(),
                    item.getProductId(), item.getWarehouseId(), item.getQty(), item.getPrice(),
                    doc.getBizDate());
        }

        // ③ 生成应付:到期日 = 业务日期 + 供应商账期
        Supplier supplier = supplierMapper.selectById(doc.getSupplierId());
        if (payableMapper.existsByDocTypeAndDocId("PURCHASE_IN", doc.getId()) > 0) {
            throw new BusinessException("该入库单已经生成应付账款");
        }
        if (items.isEmpty()) {
            throw new BusinessException("入库单明细不能为空");
        }
        Payable payable = new Payable();
        payable.setSupplierId(doc.getSupplierId());
        payable.setDocType("PURCHASE_IN");
        payable.setDocId(doc.getId());
        payable.setDocNo(doc.getDocNo());
        payable.setBizDate(doc.getBizDate());
        payable.setDueDate(doc.getBizDate().plusDays(supplier.getPaymentTermDays()));
        payable.setAmount(totalAmount(items));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setStatus("UNSETTLED");
        payableMapper.insert(payable);

        // ⑤ 操作日志(审计留痕)
        String detail = "{\"amount\":" + totalAmount(items) + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "purchase_inbound", "AUDIT",
                "PURCHASE_IN", doc.getId(), doc.getDocNo(), detail, ip);
    }

    private BigDecimal totalAmount(List<PurchaseInboundItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseInboundItem item : items) {
            total = total.add(item.getAmount());
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private PurchaseInbound requireDoc(Long id) {
        PurchaseInbound doc = inboundMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("采购入库单不存在");
        }
        return doc;
    }

    /**
     * 检查指定采购订单是否已有入库单
     */
    public boolean hasInboundByOrderId(Long orderId) {
        return inboundMapper.existsByOrderId(orderId);
    }

    /**
     * 根据ID获取入库单单号
     */
    public String getDocNo(Long id) {
        PurchaseInbound doc = inboundMapper.selectById(id);
        return doc != null ? doc.getDocNo() : null;
    }

    /**
     * 根据采购订单ID查询关联的入库单
     */
    public List<PurchaseInbound> selectByOrderId(Long orderId) {
        return inboundMapper.selectByOrderId(orderId);
    }
}
