package com.erp.module.inventory.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.inventory.entity.InventoryTransfer;
import com.erp.module.inventory.service.InventoryTransferService;
import com.erp.module.inventory.dto.InventoryTransferDtos;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 库存调拨单Controller
 */
@RestController
@RequestMapping("/inventory/transfers")
@RequiredArgsConstructor
public class InventoryTransferController {

    private final InventoryTransferService inventoryTransferService;
    private final SystemAuthorizationService authorizationService;

    /**
     * 分页查询库存调拨单
     */
    @GetMapping
    public Result<PageResult<InventoryTransferDtos.ListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));

        PageResult<InventoryTransferDtos.ListResponse> result = inventoryTransferService.page(
                page, size, keyword, status, warehouseId);
        return Result.success(result);
    }

    /**
     * 获取库存调拨单详情
     */
    @GetMapping("/{id}")
    public Result<InventoryTransferDtos.DetailResponse> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        InventoryTransferDtos.DetailResponse detail = inventoryTransferService.detail(id);
        return Result.success(detail);
    }

    /**
     * 创建库存调拨单（草稿）
     */
    @PostMapping
    public Result<Long> create(@RequestBody InventoryTransferDtos.CreateRequest request, HttpServletRequest httpRequest) {
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        authorizationService.requireInventoryWrite(currentUser);
        Long transferId = inventoryTransferService.create(request, currentUser);
        return Result.success(transferId);
    }

    /**
     * 审核库存调拨单
     */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody InventoryTransferDtos.AuditRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        inventoryTransferService.audit(id, currentUser, httpRequest.getRemoteAddr());
        return Result.success();
    }

    /**
     * 完成库存调拨
     */
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, @RequestBody InventoryTransferDtos.CompleteRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        inventoryTransferService.complete(id, currentUser, httpRequest.getRemoteAddr());
        return Result.success();
    }

    /**
     * 取消库存调拨
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody InventoryTransferDtos.CancelRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        inventoryTransferService.cancel(id, currentUser, httpRequest.getRemoteAddr());
        return Result.success();
    }

    /**
     * 根据仓库ID查询调拨列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<InventoryTransferDtos.WarehouseResponse>> listByWarehouse(@PathVariable Long warehouseId, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        List<InventoryTransferDtos.WarehouseResponse> result = inventoryTransferService.listByWarehouse(warehouseId);
        return Result.success(result);
    }

    /**
     * 获取调拨统计信息
     */
    @GetMapping("/stats")
    public Result<InventoryTransferDtos.StatsResponse> getStats(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        InventoryTransferDtos.StatsResponse stats = inventoryTransferService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取待处理的调拨单数量
     */
    @GetMapping("/stats/draft-count")
    public Result<Integer> getDraftCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryTransferService.getStats().getDraftCount();
        return Result.success(count);
    }

    /**
     * 获取已审核的调拨单数量
     */
    @GetMapping("/stats/audit-count")
    public Result<Integer> getAuditCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryTransferService.getStats().getAuditCount();
        return Result.success(count);
    }

    /**
     * 获取已完成的调拨单数量
     */
    @GetMapping("/stats/completed-count")
    public Result<Integer> getCompletedCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryTransferService.getStats().getCompletedCount();
        return Result.success(count);
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}