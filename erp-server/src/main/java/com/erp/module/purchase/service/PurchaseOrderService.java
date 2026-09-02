package com.erp.module.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.mapper.ProductMapper;
import com.erp.module.masterdata.mapper.SupplierMapper;
import com.erp.module.masterdata.mapper.WarehouseMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.purchase.entity.PurchaseOrder;
import com.erp.module.purchase.entity.PurchaseOrderItem;
import com.erp.module.purchase.mapper.PurchaseOrderMapper;
import com.erp.module.purchase.mapper.PurchaseOrderItemMapper;
import com.erp.module.purchase.dto.PurchaseOrderDtos;
import com.erp.module.purchase.dto.PurchaseInboundDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采购订单(P201):草稿创建 + 审核
 * 审核 = 单一大事务:抢占状态机 → 审计字段 → 操作日志
 * 任一步失败整体回滚
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderItemMapper itemMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final ProductMapper productMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;
    private final PurchaseInboundService purchaseInboundService;

    /** 分页列表:按单号/供应商/状态过滤 */
    public PageResult<PurchaseOrderDtos.ListResponse> page(long page, long size, String keyword, String status, Long supplierId) {
        Page<PurchaseOrder> result = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<PurchaseOrder>lambdaQuery()
                        .like(keyword != null && !keyword.isEmpty(), PurchaseOrder::getDocNo, keyword)
                        .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                        .eq(status != null && !status.isEmpty(), PurchaseOrder::getStatus, status)
                        .orderByDesc(PurchaseOrder::getId));

        List<PurchaseOrderDtos.ListResponse> listResponses = result.getRecords().stream()
                .map(this::toListResponse)
                .toList();

        return new PageResult<>(result.getTotal(), listResponses);
    }

    /** 单据详情(主表+明细) */
    public PurchaseOrderDtos.DetailResponse detail(Long id) {
        PurchaseOrder doc = requireDoc(id);
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(id);
        return new PurchaseOrderDtos.DetailResponse(doc, items);
    }

    /** 创建草稿:校验档案引用与状态,服务端计算金额 */
    @Transactional
    public Long create(PurchaseOrderDtos.CreateRequest request, TokenStore.LoginUser user) {
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

        PurchaseOrder doc = new PurchaseOrder();
        doc.setDocNo(docSequenceService.nextDocNo("PO", "PO", LocalDate.now().format(PERIOD)));
        doc.setSupplierId(request.getSupplierId());
        doc.setWarehouseId(request.getWarehouseId());
        doc.setBizDate(request.getBizDate() != null ? request.getBizDate() : LocalDate.now());
        doc.setStatus("DRAFT");
        doc.setRemark(request.getRemark() == null ? "" : request.getRemark());
        doc.setCreatedBy(user.userId());
        orderMapper.insert(doc);

        int lineNo = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderDtos.ItemInput input : request.getItems()) {
            lineNo++;
            Product product = productMapper.selectById(input.getProductId());
            if (product == null || product.getIsActive() == 0) {
                throw new BusinessException("第 " + lineNo + " 行商品不存在或已停用");
            }

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setOrderId(doc.getId());
            item.setLineNo(lineNo);
            item.setProductId(input.getProductId());
            item.setQty(input.getQty());
            item.setPrice(input.getPrice());
            item.setAmount(input.getQty().multiply(input.getPrice()).setScale(2, RoundingMode.HALF_UP));
            item.setNote(input.getNote() == null ? "" : input.getNote());
            itemMapper.insert(item);

            totalAmount = totalAmount.add(item.getAmount());
        }

        doc.setTotalAmount(totalAmount);
        orderMapper.updateById(doc);

        return doc.getId();
    }

    /**
     * 审核 = 单一大事务:
     * ① 原子抢占 DRAFT→AUDITED ② 审计字段 ③ 操作日志
     */
    @Transactional
    public void audit(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击审核只有一次生效,失败者读到已审状态报错回滚
        if (orderMapper.claimAudit(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法审核");
        }
        PurchaseOrder doc = requireDoc(id);

        // ② 更新审核字段
        doc.setStatus("AUDITED");
        doc.setAuditBy(user.userId());
        doc.setAuditAt(LocalDateTime.now());
        orderMapper.updateById(doc);

        // ③ 操作日志(审计留痕)
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(id);
        String detail = "{\"amount\":" + doc.getTotalAmount() + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "purchase_order", "AUDIT",
                "PO", id, doc.getDocNo(), detail, ip);
    }

    /**
     * 驳回 = 单一大事务:
     * ① 原子抢占 DRAFT→VOID ② 审计字段 ③ 操作日志
     */
    @Transactional
    public void reject(Long id, TokenStore.LoginUser user, String ip) {
        // ① 抢占状态机:并发双击驳回只有一次生效
        if (orderMapper.claimReject(id, user.userId()) == 0) {
            throw new BusinessException("单据不存在或不是草稿状态,无法驳回");
        }
        PurchaseOrder doc = requireDoc(id);

        // ② 更新驳回字段
        doc.setStatus("VOID");
        doc.setRejectBy(user.userId());
        doc.setRejectAt(LocalDateTime.now());
        orderMapper.updateById(doc);

        // ③ 操作日志(审计留痕)
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(id);
        String detail = "{\"amount\":" + doc.getTotalAmount() + ",\"lines\":" + items.size() + "}";
        operationLogService.record(user, "purchase_order", "REJECT",
                "PO", id, doc.getDocNo(), detail, ip);
    }

    /** 查询指定供应商的采购订单 */
    public List<PurchaseOrder> findBySupplierId(Long supplierId) {
        return orderMapper.selectBySupplierId(supplierId);
    }

    /** 查询指定仓库的采购订单 */
    public List<PurchaseOrder> findByWarehouseId(Long warehouseId) {
        return orderMapper.selectByWarehouseId(warehouseId);
    }

    /** 查询指定日期范围内的采购订单 */
    public List<PurchaseOrder> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return orderMapper.selectByDateRange(startDate, endDate);
    }

    /** 查询待审核的采购订单数量 */
    public int countDraftOrders() {
        return orderMapper.countDraftOrders();
    }

    /** 查询已审核未入库的采购订单数量 */
    public int countUnreceivedOrders() {
        return orderMapper.countUnreceivedOrders();
    }

    /** 更新采购数量 */
    @Transactional
    public void updateQty(Long orderId, PurchaseOrderDtos.UpdateQtyRequest request, TokenStore.LoginUser user) {
        PurchaseOrder doc = requireDoc(orderId);
        if (!"DRAFT".equals(doc.getStatus())) {
            throw new BusinessException("只有草稿状态的订单可以修改数量");
        }

        // 重新计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderDtos.ItemUpdate itemUpdate : request.getItems()) {
            PurchaseOrderItem item = itemMapper.selectById(itemUpdate.getItemId());
            if (item == null || !item.getOrderId().equals(orderId)) {
                throw new BusinessException("明细不存在: " + itemUpdate.getItemId());
            }

            // 更新数量
            item.setQty(itemUpdate.getQty());
            item.setAmount(item.getQty().multiply(item.getPrice()).setScale(2, RoundingMode.HALF_UP));
            itemMapper.updateById(item);

            totalAmount = totalAmount.add(item.getAmount());
        }

        // 更新订单总金额
        doc.setTotalAmount(totalAmount);
        orderMapper.updateById(doc);
    }

    private PurchaseOrder requireDoc(Long id) {
        PurchaseOrder doc = orderMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("采购订单不存在");
        }
        return doc;
    }

    private PurchaseOrderDtos.ListResponse toListResponse(PurchaseOrder doc) {
        PurchaseOrderDtos.ListResponse response = new PurchaseOrderDtos.ListResponse();
        response.setId(doc.getId());
        response.setDocNo(doc.getDocNo());
        response.setSupplierId(doc.getSupplierId());

        // 设置供应商名称
        Supplier supplier = supplierMapper.selectById(doc.getSupplierId());
        response.setSupplierName(supplier != null ? supplier.getName() : "");

        response.setWarehouseId(doc.getWarehouseId());

        // 设置仓库名称
        Warehouse warehouse = warehouseMapper.selectById(doc.getWarehouseId());
        response.setWarehouseName(warehouse != null ? warehouse.getName() : "");

        response.setStatus(doc.getStatus());
        response.setTotalAmount(doc.getTotalAmount());
        response.setBizDate(doc.getBizDate());
        response.setRemark(doc.getRemark());
        response.setCreatedAt(doc.getCreatedAt());
        response.setUpdatedAt(doc.getUpdatedAt());
        return response;
    }

    /**
     * 从采购订单创建入库单
     */
    @Transactional
    public PurchaseOrderDtos.CreateFromOrderResponse createInboundFromOrder(Long orderId, TokenStore.LoginUser user, String ip) {
        PurchaseOrder order = requireDoc(orderId);
        if (!"AUDITED".equals(order.getStatus())) {
            throw new BusinessException("只有已审核的采购订单才能创建入库单");
        }

        // 检查是否已经创建过入库单
        boolean hasInbound = purchaseInboundService.hasInboundByOrderId(orderId);
        if (hasInbound) {
            throw new BusinessException("该采购订单已经创建了入库单");
        }

        // 创建入库单
        PurchaseInboundDtos.CreateRequest inboundRequest = new PurchaseInboundDtos.CreateRequest();
        inboundRequest.setSupplierId(order.getSupplierId());
        inboundRequest.setWarehouseId(order.getWarehouseId());
        inboundRequest.setBizDate(order.getBizDate());
        inboundRequest.setRemark("从采购订单 " + order.getDocNo() + " 创建");

        // 获取采购订单明细
        List<PurchaseOrderItem> orderItems = itemMapper.selectByOrderId(orderId);
        List<PurchaseInboundDtos.ItemInput> inboundItems = orderItems.stream()
                .map(item -> {
                    PurchaseInboundDtos.ItemInput inboundItem = new PurchaseInboundDtos.ItemInput();
                    inboundItem.setProductId(item.getProductId());
                    inboundItem.setQty(item.getQty());
                    inboundItem.setPrice(item.getPrice());
                    inboundItem.setNote(item.getNote());
                    return inboundItem;
                })
                .toList();

        inboundRequest.setItems(inboundItems);

        // 创建入库单
        Long inboundId = purchaseInboundService.create(inboundRequest, user);

        // 返回结果
        PurchaseOrderDtos.CreateFromOrderResponse response = new PurchaseOrderDtos.CreateFromOrderResponse();
        response.setOrderId(orderId);
        response.setOrderDocNo(order.getDocNo());
        response.setInboundId(inboundId);
        response.setInboundDocNo(purchaseInboundService.getDocNo(inboundId));

        // 记录操作日志
        operationLogService.record(user, "purchase_order", "CREATE_INBOUND",
                "PO", orderId, order.getDocNo(),
                "{\"inboundId\":" + inboundId + ",\"inboundDocNo\":\"" + response.getInboundDocNo() + "\"}",
                ip);

        return response;
    }

    /**
     * 检查采购订单是否已全部入库
     */
    public boolean isOrderFullyReceived(Long orderId) {
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(orderId);
        if (items.isEmpty()) {
            return true;
        }

        for (PurchaseOrderItem item : items) {
            if (item.getReceivedQty().compareTo(item.getQty()) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取采购订单的入库进度
     */
    public PurchaseOrderDtos.ReceivedProgressResponse getReceivedProgress(Long orderId) {
        List<PurchaseOrderItem> items = itemMapper.selectByOrderId(orderId);

        BigDecimal totalOrdered = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (PurchaseOrderItem item : items) {
            totalOrdered = totalOrdered.add(item.getQty());
            totalReceived = totalReceived.add(item.getReceivedQty());
        }

        PurchaseOrderDtos.ReceivedProgressResponse response = new PurchaseOrderDtos.ReceivedProgressResponse();
        response.setOrderId(orderId);
        response.setTotalOrdered(totalOrdered);
        response.setTotalReceived(totalReceived);
        response.setRemaining(totalOrdered.subtract(totalReceived));
        response.setProgressRate(totalOrdered.compareTo(BigDecimal.ZERO) == 0 ?
                BigDecimal.ZERO : totalReceived.divide(totalOrdered, 4, RoundingMode.HALF_UP));

        return response;
    }
}