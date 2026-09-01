package com.erp.module.inventory.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.TokenStore;
import com.erp.module.inventory.entity.InventoryTransfer;
import com.erp.module.inventory.service.InventoryTransferService;
import com.erp.module.inventory.dto.InventoryTransferDtos;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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

    /**
     * 分页查询库存调拨单
     */
    @GetMapping
    public Result<PageResult<InventoryTransferDtos.ListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {

        PageResult<InventoryTransferDtos.ListResponse> result = inventoryTransferService.page(
                page, size, keyword, status, warehouseId);
        return Result.success(result);
    }

    /**
     * 获取库存调拨单详情
     */
    @GetMapping("/{id}")
    public Result<InventoryTransferDtos.DetailResponse> detail(@PathVariable Long id) {
        InventoryTransferDtos.DetailResponse detail = inventoryTransferService.detail(id);
        return Result.success(detail);
    }

    /**
     * 创建库存调拨单（草稿）
     */
    @PostMapping
    public Result<Long> create(@RequestBody InventoryTransferDtos.CreateRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long transferId = inventoryTransferService.create(request, currentUser);
        return Result.success(transferId);
    }

    /**
     * 审核库存调拨单
     */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody InventoryTransferDtos.AuditRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryTransferService.audit(id, currentUser, request.getIp());
        return Result.success();
    }

    /**
     * 完成库存调拨
     */
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id, @RequestBody InventoryTransferDtos.CompleteRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryTransferService.complete(id, currentUser, request.getIp());
        return Result.success();
    }

    /**
     * 取消库存调拨
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody InventoryTransferDtos.CancelRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryTransferService.cancel(id, currentUser, request.getIp());
        return Result.success();
    }

    /**
     * 根据仓库ID查询调拨列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<InventoryTransferDtos.WarehouseResponse>> listByWarehouse(@PathVariable Long warehouseId) {
        List<InventoryTransferDtos.WarehouseResponse> result = inventoryTransferService.listByWarehouse(warehouseId);
        return Result.success(result);
    }

    /**
     * 获取调拨统计信息
     */
    @GetMapping("/stats")
    public Result<InventoryTransferDtos.StatsResponse> getStats() {
        InventoryTransferDtos.StatsResponse stats = inventoryTransferService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取待处理的调拨单数量
     */
    @GetMapping("/stats/draft-count")
    public Result<Integer> getDraftCount() {
        Integer count = inventoryTransferService.getStats().getDraftCount();
        return Result.success(count);
    }

    /**
     * 获取已审核的调拨单数量
     */
    @GetMapping("/stats/audit-count")
    public Result<Integer> getAuditCount() {
        Integer count = inventoryTransferService.getStats().getAuditCount();
        return Result.success(count);
    }

    /**
     * 获取已完成的调拨单数量
     */
    @GetMapping("/stats/completed-count")
    public Result<Integer> getCompletedCount() {
        Integer count = inventoryTransferService.getStats().getCompletedCount();
        return Result.success(count);
    }
}