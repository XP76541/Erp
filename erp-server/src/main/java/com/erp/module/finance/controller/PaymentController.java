package com.erp.module.finance.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.erp.common.Result;
import com.erp.module.finance.dto.PaymentDtos;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.service.PaymentService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    private static TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }

    @GetMapping
    public Result<IPage<Payment>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(paymentService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<PaymentDtos.DetailResponse> detail(@PathVariable Long id) {
        return Result.ok(paymentService.detail(id));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PaymentDtos.CreateRequest req, HttpServletRequest httpRequest) {
        return Result.ok(paymentService.create(req, currentUser(httpRequest)));
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, HttpServletRequest httpRequest) {
        paymentService.audit(id, currentUser(httpRequest), httpRequest.getRemoteAddr());
        return Result.ok();
    }
}