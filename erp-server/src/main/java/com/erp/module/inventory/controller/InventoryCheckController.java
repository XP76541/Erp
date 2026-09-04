package com.erp.module.inventory.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.inventory.entity.InventoryCheck;
import com.erp.module.inventory.service.InventoryCheckService;
import com.erp.module.inventory.dto.InventoryCheckDtos;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import java.util.List;

/**
 * 库存盘点单Controller
 */
@RestController
@RequestMapping("/inventory/checks")
@RequiredArgsConstructor
public class InventoryCheckController {

    private final InventoryCheckService inventoryCheckService;
    private final SystemAuthorizationService authorizationService;

    /**
     * 分页查询库存盘点单
     */
    @GetMapping
    public Result<PageResult<InventoryCheckDtos.ListResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size,
            HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));

        PageResult<InventoryCheckDtos.ListResponse> result = inventoryCheckService.page(
                page, size, keyword, status, warehouseId);
        return Result.success(result);
    }

    /**
     * 获取库存盘点单详情
     */
    @GetMapping("/{id}")
    public Result<InventoryCheckDtos.DetailResponse> detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        InventoryCheckDtos.DetailResponse detail = inventoryCheckService.detail(id);
        return Result.success(detail);
    }

    /**
     * 创建库存盘点单
     */
    @PostMapping
    public Result<Long> create(@RequestBody InventoryCheckDtos.CreateRequest request, HttpServletRequest httpRequest) {
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        authorizationService.requireInventoryWrite(currentUser);
        Long checkId = inventoryCheckService.create(request, currentUser);
        return Result.success(checkId);
    }

    /**
     * 开始盘点
     */
    @PutMapping("/{id}/start-check")
    public Result<Void> startCheck(@PathVariable Long id, @RequestBody InventoryCheckDtos.StartCheckRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        String ip = httpRequest.getRemoteAddr();
        inventoryCheckService.startCheck(id, currentUser, ip);
        return Result.success();
    }

    /**
     * 提交盘点结果
     */
    @PutMapping("/{id}/submit-result")
    public Result<Void> submitResult(@PathVariable Long id,
                                   @RequestBody InventoryCheckDtos.SubmitResultRequest request,
                                   HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        inventoryCheckService.submitResult(id, request, currentUser, httpRequest.getRemoteAddr());
        return Result.success();
    }

    /**
     * 审核盘点单
     */
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody InventoryCheckDtos.AuditRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        String ip = httpRequest.getRemoteAddr();
        inventoryCheckService.audit(id, currentUser, ip);
        return Result.success();
    }

    /**
     * 取消盘点单
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestBody InventoryCheckDtos.CancelRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryWrite(currentUser(httpRequest));
        TokenStore.LoginUser currentUser = currentUser(httpRequest);
        String ip = httpRequest.getRemoteAddr();
        inventoryCheckService.cancel(id, currentUser, ip);
        return Result.success();
    }

    /**
     * 根据仓库查询盘点列表
     */
    @GetMapping("/warehouse/{warehouseId}")
    public Result<List<InventoryCheckDtos.WarehouseResponse>> listByWarehouse(@PathVariable Long warehouseId, HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        List<InventoryCheckDtos.WarehouseResponse> result = inventoryCheckService.listByWarehouse(warehouseId);
        return Result.success(result);
    }

    /**
     * 获取盘点统计
     */
    @GetMapping("/stats")
    public Result<InventoryCheckDtos.StatsResponse> getStats(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        InventoryCheckDtos.StatsResponse stats = inventoryCheckService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取待盘点的单据数量
     */
    @GetMapping("/stats/draft-count")
    public Result<Integer> getDraftCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryCheckService.getStats().getDraftCount();
        return Result.success(count);
    }

    /**
     * 获取盘点中的单据数量
     */
    @GetMapping("/stats/checking-count")
    public Result<Integer> getCheckingCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryCheckService.getStats().getCheckingCount();
        return Result.success(count);
    }

    /**
     * 获取已盘点的单据数量
     */
    @GetMapping("/stats/audited-count")
    public Result<Integer> getAuditedCount(HttpServletRequest httpRequest) {
        authorizationService.requireInventoryRead(currentUser(httpRequest));
        Integer count = inventoryCheckService.getStats().getAuditedCount();
        return Result.success(count);
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}