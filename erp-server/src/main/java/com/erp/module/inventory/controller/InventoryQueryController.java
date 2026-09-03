package com.erp.module.inventory.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.inventory.dto.InventoryQueryDtos;
import com.erp.module.inventory.service.InventoryQueryService;
import lombok.RequiredArgsConstructor;
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
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(inventoryQueryService.stocks(warehouseId, productId, categoryId, page, size));
    }

    @GetMapping("/ledgers")
    public Result<PageResult<InventoryQueryDtos.LedgerResponse>> ledgers(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String docType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.success(inventoryQueryService.ledgers(warehouseId, productId, docType, startDate, endDate, page, size));
    }
}
