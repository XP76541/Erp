package com.erp.module.masterdata.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 客户档案(F101/US-102):列表、新增、修改、停启用
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    public record StatusRequest(@NotNull Boolean active) {
    }

    @GetMapping
    public Result<PageResult<Customer>> page(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword) {
        return Result.ok(customerService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody Customer customer) {
        return Result.ok(customerService.create(customer));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody Customer customer) {
        customer.setId(id);
        customerService.update(customer);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        customerService.toggleStatus(id, body.active());
        return Result.ok();
    }
}
