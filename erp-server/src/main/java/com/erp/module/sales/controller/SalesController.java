package com.erp.module.sales.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.sales.dto.SalesOrderDtos;
import com.erp.module.sales.dto.SalesOrderDtos.CreateRequest;
import com.erp.module.sales.dto.SalesOrderDtos.AuditRequest;
import com.erp.module.sales.dto.SalesOutboundDtos;
import com.erp.module.sales.dto.SalesOutboundDtos.CreateRequest as OutboundCreateRequest;
import com.erp.module.sales.entity.SalesOrder;
import com.erp.module.sales.service.SalesOrderService;
import com.erp.module.sales.service.SalesOutboundService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售订单和出库单控制器
 */
@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesOrderService orderService;
    private final SalesOutboundService outboundService;

    // ===== 销售订单 =====

    /**
     * 销售订单分页查询
     */
    @GetMapping("/orders")
    public Result<PageResult<SalesOrderDtos.ListResponse>> pageOrders(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {
        return Result.ok(orderService.page(page, size, keyword, status, customerId));
    }

    /**
     * 销售订单详情
     */
    @GetMapping("/orders/{id}")
    public Result<SalesOrderDtos.DetailResponse> orderDetail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    /**
     * 创建销售订单
     */
    @PostMapping("/orders")
    public Result<Long> createOrder(@Valid @RequestBody CreateRequest request, HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        return Result.ok(orderService.create(request, user));
    }

    /**
     * 审核销售订单
     */
    @PutMapping("/orders/{id}/audit")
    public Result<Void> auditOrder(@PathVariable Long id,
                                  @Valid @RequestBody AuditRequest request,
                                  HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        if ("audit".equals(request.getAction())) {
            orderService.audit(id, user, httpRequest.getRemoteAddr());
        } else if ("reject".equals(request.getAction())) {
            orderService.reject(id, user, httpRequest.getRemoteAddr());
        } else {
            throw new IllegalArgumentException("操作类型必须是 audit 或 reject");
        }
        return Result.ok();
    }

    // ===== 销售出库单 =====

    /**
     * 销售出库单分页查询
     */
    @GetMapping("/outbounds")
    public Result<PageResult<SalesOutboundDtos.ListResponse>> pageOutbounds(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId) {
        return Result.ok(outboundService.page(page, size, keyword, status, customerId));
    }

    /**
     * 销售出库单详情
     */
    @GetMapping("/outbounds/{id}")
    public Result<SalesOutboundDtos.DetailResponse> outboundDetail(@PathVariable Long id) {
        return Result.ok(outboundService.detail(id));
    }

    /**
     * 从销售订单创建出库单
     */
    @PostMapping("/outbounds/from-order")
    public Result<SalesOutboundDtos.CreateFromOrderResponse> createOutboundFromOrder(
            @RequestParam Long orderId,
            @Valid @RequestBody OutboundCreateRequest request,
            HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        return Result.ok(outboundService.createFromOrder(orderId, request, user));
    }

    /**
     * 审核销售出库单
     */
    @PutMapping("/outbounds/{id}/audit")
    public Result<Void> auditOutbound(@PathVariable Long id,
                                     @Valid @RequestBody AuditRequest request,
                                     HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        if ("audit".equals(request.getAction())) {
            outboundService.audit(id, user, httpRequest.getRemoteAddr());
        } else if ("reject".equals(request.getAction())) {
            outboundService.reject(id, user, httpRequest.getRemoteAddr());
        } else {
            throw new IllegalArgumentException("操作类型必须是 audit 或 reject");
        }
        return Result.ok();
    }

    // ===== 统计接口 =====

    /**
     * 获取销售订单统计
     */
    @GetMapping("/stats/orders")
    public Result<Object> orderStats(HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        return Result.ok(new Object() {
            public int draftCount = orderService.countDraftOrders();
            public int unshippedCount = orderService.countUnshippedOrders();
        });
    }

    /**
     * 获取销售出库单统计
     */
    @GetMapping("/stats/outbounds")
    public Result<Object> outboundStats(HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        return Result.ok(new Object() {
            public int draftCount = outboundService.countDraftOutbounds();
            public int unpaidCount = outboundService.countUnpaidOutbounds();
        });
    }

    /**
     * 查询客户的销售订单
     */
    @GetMapping("/orders/customer/{customerId}")
    public Result<PageResult<SalesOrderDtos.ListResponse>> ordersByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.page(page, size, null, null, customerId));
    }

    /**
     * 查询销售人员的销售订单
     */
    @GetMapping("/orders/salesperson/{salespersonId}")
    public Result<List<SalesOrderDtos.ListResponse>> ordersBySalesperson(
            @PathVariable Long salespersonId) {
        return Result.ok(orderService.findBySalespersonId(salespersonId));
    }

    /**
     * 查询指定日期范围内的销售订单
     */
    @GetMapping("/orders/date-range")
    public Result<List<SalesOrderDtos.ListResponse>> ordersByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Result.ok(orderService.findByDateRange(startDate, endDate));
    }

    /**
     * 查询客户的销售出库单
     */
    @GetMapping("/outbounds/customer/{customerId}")
    public Result<List<SalesOutboundDtos.ListResponse>> outboundsByCustomer(
            @PathVariable Long customerId) {
        return Result.ok(outboundService.findByCustomerId(customerId));
    }

    /**
     * 查询指定仓库的销售出库单
     */
    @GetMapping("/outbounds/warehouse/{warehouseId}")
    public Result<List<SalesOutboundDtos.ListResponse>> outboundsByWarehouse(
            @PathVariable Long warehouseId) {
        return Result.ok(outboundService.findByWarehouseId(warehouseId));
    }

    /**
     * 查询指定日期范围内的销售出库单
     */
    @GetMapping("/outbounds/date-range")
    public Result<List<SalesOutboundDtos.ListResponse>> outboundsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        // Note: findByDateRange method needs to be added to SalesOutboundService
        return Result.ok(List.of()); // Placeholder
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}