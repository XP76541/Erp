package com.erp.module.masterdata.controller;

import com.erp.common.Result;
import com.erp.module.masterdata.entity.Warehouse;
import com.erp.module.masterdata.service.WarehouseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 仓库档案(F101/US-101):全量列表、新增、修改、停启用
 */
@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    public record StatusRequest(@NotNull Boolean active) {
    }

    @GetMapping
    public Result<List<Warehouse>> listAll() {
        return Result.ok(warehouseService.listAll());
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody Warehouse warehouse) {
        return Result.ok(warehouseService.create(warehouse));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        warehouseService.update(warehouse);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        warehouseService.toggleStatus(id, body.active());
        return Result.ok();
    }
}
