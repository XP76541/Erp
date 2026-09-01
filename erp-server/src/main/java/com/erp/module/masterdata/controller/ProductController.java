package com.erp.module.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.masterdata.entity.Product;
import com.erp.module.masterdata.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品档案(F101/US-101):列表、新增、修改、停启用
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    public record StatusRequest(@NotNull Boolean active) {
    }

    @GetMapping
    public Result<PageResult<Product>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(productService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody Product product) {
        return Result.ok(productService.create(product));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Product product) {
        product.setId(id);
        productService.update(product);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        productService.toggleStatus(id, body.active());
        return Result.ok();
    }
}
