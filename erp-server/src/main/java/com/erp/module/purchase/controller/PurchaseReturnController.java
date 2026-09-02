package com.erp.module.purchase.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.purchase.dto.PurchaseReturnDtos;
import com.erp.module.purchase.service.PurchaseReturnService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase-returns")
@RequiredArgsConstructor
public class PurchaseReturnController {
    private final PurchaseReturnService returnService;

    @GetMapping
    public Result<PageResult<PurchaseReturnDtos.ListResponse>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long supplierId) {
        return Result.ok(returnService.page(page, size, keyword, status, supplierId));
    }

    @GetMapping("/{id}")
    public Result<PurchaseReturnDtos.DetailResponse> detail(@PathVariable Long id) {
        return Result.ok(returnService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PurchaseReturnDtos.CreateRequest request, HttpServletRequest httpRequest) {
        return Result.ok(returnService.create(request, currentUser(httpRequest)));
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, HttpServletRequest httpRequest) {
        returnService.audit(id, currentUser(httpRequest), httpRequest.getRemoteAddr());
        return Result.ok();
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}
