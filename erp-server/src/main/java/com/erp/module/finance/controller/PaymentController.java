package com.erp.module.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.finance.dto.PaymentDtos;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.service.PaymentService;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final SystemAuthorizationService authorizationService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PaymentDtos.PaymentCreateRequest request, HttpServletRequest httpRequest) {
        authorizationService.requireFinanceAccess(currentUser(httpRequest));
        Payment payment = paymentService.createDraft(request, currentUser(httpRequest));
        return Result.ok(payment.getId());
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, HttpServletRequest httpRequest) {
        authorizationService.requireFinanceAccess(currentUser(httpRequest));
        paymentService.audit(id, currentUser(httpRequest), httpRequest.getRemoteAddr());
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<PaymentDtos.PaymentListResponse>> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) java.time.LocalDate startDate,
            @RequestParam(required = false) java.time.LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest httpRequest) {
        PaymentDtos.PaymentListRequest params = new PaymentDtos.PaymentListRequest();
        params.setSupplierId(supplierId); params.setStatus(status);
        params.setStartDate(startDate); params.setEndDate(endDate);
        params.setPage(page); params.setSize(size);
        authorizationService.requireFinanceAccess(currentUser(httpRequest));
        return Result.ok(paymentService.getPayments(params));
    }

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }
}
