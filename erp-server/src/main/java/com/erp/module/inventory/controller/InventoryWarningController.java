package com.erp.module.inventory.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.TokenStore;
import com.erp.module.inventory.service.InventoryWarningService;
import com.erp.module.inventory.dto.InventoryWarningDtos;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 库存预警Controller
 */
@RestController
@RequestMapping("/inventory/warnings")
@RequiredArgsConstructor
public class InventoryWarningController {

    private final InventoryWarningService inventoryWarningService;

    /**
     * 分页查询库存预警
     */
    @GetMapping
    public Result<PageResult<InventoryWarningDtos.ListResponse>> list(
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {

        PageResult<InventoryWarningDtos.ListResponse> result = inventoryWarningService.page(
                page, size, warningType, warehouseId, productId, isActive);
        return Result.success(result);
    }

    /**
     * 获取库存预警详情
     */
    @GetMapping("/{id}")
    public Result<InventoryWarningDtos.DetailResponse> detail(@PathVariable Long id) {
        InventoryWarningDtos.DetailResponse detail = inventoryWarningService.detail(id);
        return Result.success(detail);
    }

    /**
     * 获取激活的预警
     */
    @GetMapping("/active")
    public Result<List<InventoryWarningDtos.ActiveResponse>> getActiveWarnings(
            @RequestParam(required = false) Long warehouseId) {

        List<InventoryWarningDtos.ActiveResponse> result = inventoryWarningService.getActiveWarnings(warehouseId);
        return Result.success(result);
    }

    /**
     * 根据预警类型查询
     */
    @GetMapping("/type/{warningType}")
    public Result<List<InventoryWarningDtos.ActiveResponse>> getWarningsByType(
            @PathVariable String warningType,
            @RequestParam(required = false) Long warehouseId) {

        List<InventoryWarningDtos.ActiveResponse> result = inventoryWarningService.getWarningsByType(warningType, warehouseId);
        return Result.success(result);
    }

    /**
     * 解决预警
     */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id,
                               @RequestParam(required = false) String remark) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryWarningService.resolveWarning(id, currentUser, remark);
        return Result.success();
    }

    /**
     * 批量解决预警
     */
    @PutMapping("/batch-resolve")
    public Result<Void> batchResolve(@RequestBody List<Long> ids) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryWarningService.batchResolveWarnings(ids, currentUser);
        return Result.success();
    }

    /**
     * 获取预警统计
     */
    @GetMapping("/stats")
    public Result<InventoryWarningDtos.StatsResponse> getStats() {
        InventoryWarningDtos.StatsResponse stats = inventoryWarningService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取逾期未解决的预警
     */
    @GetMapping("/overdue")
    public Result<List<InventoryWarningDtos.OverdueResponse>> getOverdueWarnings() {
        List<InventoryWarningDtos.OverdueResponse> result = inventoryWarningService.getOverdueWarnings();
        return Result.success(result);
    }

    /**
     * 获取预警配置
     */
    @GetMapping("/warning-configs")
    public Result<List<InventoryWarningDtos.WarningConfigResponse>> getWarningConfigs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId) {

        List<InventoryWarningDtos.WarningConfigResponse> result = inventoryWarningService.getWarningConfigs(productId, warehouseId);
        return Result.success(result);
    }

    /**
     * 创建预警配置
     */
    @PostMapping("/warning-configs")
    public Result<Long> createWarningConfig(@RequestBody InventoryWarningDtos.CreateConfigRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long configId = inventoryWarningService.createWarningConfig(request, currentUser);
        return Result.success(configId);
    }

    /**
     * 更新预警配置
     */
    @PutMapping("/warning-configs/{id}")
    public Result<Void> updateWarningConfig(@PathVariable Long id,
                                          @RequestBody InventoryWarningDtos.UpdateConfigRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryWarningService.updateWarningConfig(id, request, currentUser);
        return Result.success();
    }

    /**
     * 启用/禁用预警配置
     */
    @PutMapping("/warning-configs/{id}/toggle")
    public Result<Void> toggleWarningConfig(@PathVariable Long id,
                                          @RequestParam Boolean isActive) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryWarningService.toggleWarningConfig(id, isActive, currentUser);
        return Result.success();
    }

    /**
     * 批量启用/禁用预警配置
     */
    @PutMapping("/warning-configs/batch-toggle")
    public Result<Void> batchToggleWarningConfig(@RequestBody List<Long> ids,
                                               @RequestParam Boolean isActive) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        inventoryWarningService.batchToggleWarningConfig(ids, isActive, currentUser);
        return Result.success();
    }

    /**
     * 库存不足预警数量
     */
    @GetMapping("/stats/stock-out-count")
    public Result<Integer> getStockOutCount() {
        Integer count = inventoryWarningService.getStats().getStockOutCount();
        return Result.success(count);
    }

    /**
     * 库存超量预警数量
     */
    @GetMapping("/stats/stock-over-count")
    public Result<Integer> getStockOverCount() {
        Integer count = inventoryWarningService.getStats().getStockOverCount();
        return Result.success(count);
    }

    /**
     * 临期预警数量
     */
    @GetMapping("/stats/expiring-count")
    public Result<Integer> getExpiringCount() {
        Integer count = inventoryWarningService.getStats().getExpiringCount();
        return Result.success(count);
    }

    /**
     * 呆滞预警数量
     */
    @GetMapping("/stats/spoiled-count")
    public Result<Integer> getSpoiledCount() {
        Integer count = inventoryWarningService.getStats().getSpoiledCount();
        return Result.success(count);
    }
}