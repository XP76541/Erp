package com.erp.module.purchase.controller;

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
