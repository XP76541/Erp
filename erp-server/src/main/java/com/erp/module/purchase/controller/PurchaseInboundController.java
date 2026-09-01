package com.erp.module.purchase.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.purchase.dto.PurchaseInboundDtos;
import com.erp.module.purchase.dto.PurchaseInboundDtos.CreateRequest;
import com.erp.module.purchase.entity.PurchaseInbound;
import com.erp.module.purchase.service.PurchaseInboundService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 采购入库单(F201/US-201):分页、详情、创建草稿、审核
 */
@RestController
@RequestMapping("/purchase-inbounds")
@RequiredArgsConstructor
public class PurchaseInboundController {

    private final PurchaseInboundService inboundService;

    @GetMapping
    public Result<PageResult<PurchaseInbound>> page(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) String status) {
        return Result.ok(inboundService.page(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    public Result<PurchaseInboundDtos.DetailResponse> detail(@PathVariable Long id) {
        return Result.ok(inboundService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateRequest request, HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        return Result.ok(inboundService.create(request, user));
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        inboundService.audit(id, user, httpRequest.getRemoteAddr());
        return Result.ok();
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}
