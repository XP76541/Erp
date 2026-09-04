package com.erp.module.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.inventory.dto.InventoryQueryDtos;
import com.erp.module.inventory.service.InventoryQueryService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** 即时库存与库存流水查询 Controller */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryQueryController {
    private final InventoryQueryService inventoryQueryService;

    @GetMapping("/stocks")
    public Result<PageResult<InventoryQueryDtos.StockResponse>> stocks(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            HttpServletRequest request) {
        TokenStore.LoginUser user = currentUser(request);
        return Result.success(inventoryQueryService.stocks(warehouseId, productId, categoryId, page, size, user));
    }

    @GetMapping("/ledgers")
    public Result<PageResult<InventoryQueryDtos.LedgerResponse>> ledgers(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            HttpServletRequest request) {
        TokenStore.LoginUser user = currentUser(request);
        return Result.success(inventoryQueryService.ledgers(warehouseId, productId, docType, startDate, endDate, page, size, user));
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}