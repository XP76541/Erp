package com.erp.module.purchase.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.TokenStore;
import com.erp.module.purchase.entity.PurchaseOrder;
import com.erp.module.purchase.service.PurchaseOrderService;
import com.erp.module.purchase.dto.PurchaseOrderDtos;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 采购订单Controller
 */
@RestController
@RequestMapping("/purchase/orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * 分页查询采购订单
     */
    @GetMapping
    public Result<PageResult<PurchaseOrderDtos.ListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {

        PageResult<PurchaseOrderDtos.ListResponse> result = purchaseOrderService.page(page, size, keyword, status, supplierId);
        return Result.success(result);
    }

    /**
     * 获取采购订单详情
     */
    @GetMapping("/{id}")
    public Result<PurchaseOrderDtos.DetailResponse> detail(@PathVariable Long id) {
        PurchaseOrderDtos.DetailResponse detail = purchaseOrderService.detail(id);
        return Result.success(detail);
    }

    /**
     * 创建采购订单（草稿）
     */
    @PostMapping
    public Result<Long> create(@RequestBody PurchaseOrderDtos.CreateRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long orderId = purchaseOrderService.create(request, currentUser);
        return Result.success(orderId);
    }

    /**
     * 审核采购订单
     */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody PurchaseOrderDtos.AuditRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        purchaseOrderService.audit(id, currentUser, request.getIp());
        return Result.success();
    }

    /**
     * 驳回采购订单
     */
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody PurchaseOrderDtos.RejectRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        purchaseOrderService.reject(id, currentUser, request.getIp());
        return Result.success();
    }

    /**
     * 更新采购数量（仅限草稿状态）
     */
    @PutMapping("/{id}/qty")
    public Result<Void> updateQty(@PathVariable Long id, @RequestBody PurchaseOrderDtos.UpdateQtyRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        purchaseOrderService.updateQty(id, request, currentUser);
        return Result.success();
    }

    /**
     * 获取供应商的采购订单列表
     */
    @GetMapping("/supplier/{supplierId}")
    public Result<List<PurchaseOrderDtos.ListResponse>> getBySupplier(@PathVariable Long supplierId) {
        List<PurchaseOrder> orders = purchaseOrderService.findBySupplierId(supplierId);
        List<PurchaseOrderDtos.ListResponse> responses = orders.stream()
                .map(order -> {
                    PurchaseOrderDtos.ListResponse response = new PurchaseOrderDtos.ListResponse();
                    response.setId(order.getId());
                    response.setDocNo(order.getDocNo());
                    response.setSupplierId(order.getSupplierId());

                    // 设置供应商名称
                    response.setSupplierName("供应商" + supplierId); // 实际应该从SupplierService获取

                    response.setWarehouseId(order.getWarehouseId());
                    response.setStatus(order.getStatus());
                    response.setTotalAmount(order.getTotalAmount());
                    response.setBizDate(order.getBizDate());
                    response.setCreatedAt(order.getCreatedAt());
                    return response;
                })
                .toList();
        return Result.success(responses);
    }

    /**
     * 获取待审核订单数量
     */
    @GetMapping("/stats/draft-count")
    public Result<Integer> getDraftCount() {
        int count = purchaseOrderService.countDraftOrders();
        return Result.success(count);
    }

    /**
     * 获取未入库订单数量
     */
    @GetMapping("/stats/unreceived-count")
    public Result<Integer> getUnreceivedCount() {
        int count = purchaseOrderService.countUnreceivedOrders();
        return Result.success(count);
    }

    /**
     * 从采购订单创建入库单
     */
    @PostMapping("/{id}/create-inbound")
    public Result<PurchaseOrderDtos.CreateFromOrderResponse> createInboundFromOrder(@PathVariable Long id) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        String ip = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getRemoteAddr();
        PurchaseOrderDtos.CreateFromOrderResponse response = purchaseOrderService.createInboundFromOrder(id, currentUser, ip);
        return Result.success(response);
    }

    /**
     * 获取采购订单的入库进度
     */
    @GetMapping("/{id}/received-progress")
    public Result<PurchaseOrderDtos.ReceivedProgressResponse> getReceivedProgress(@PathVariable Long id) {
        PurchaseOrderDtos.ReceivedProgressResponse response = purchaseOrderService.getReceivedProgress(id);
        return Result.success(response);
    }

    /**
     * 检查采购订单是否已全部入库
     */
    @GetMapping("/{id}/is-fully-received")
    public Result<Boolean> isOrderFullyReceived(@PathVariable Long id) {
        boolean fullyReceived = purchaseOrderService.isOrderFullyReceived(id);
        return Result.success(fullyReceived);
    }
}