package com.erp.module.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.service.SupplierService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 供应商档案(F101/US-103):列表、新增、修改、停启用
 */
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    public record StatusRequest(@NotNull Boolean active) {
    }

    @GetMapping
    public Result<PageResult<Supplier>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(supplierService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody Supplier supplier) {
        return Result.ok(supplierService.create(supplier));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierService.update(supplier);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        supplierService.toggleStatus(id, body.active());
        return Result.ok();
    }
}
