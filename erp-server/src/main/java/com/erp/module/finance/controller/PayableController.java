package com.erp.module.finance.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.module.finance.dto.PayableDtos;
import com.erp.module.finance.service.PayableService;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.SystemAuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/finance/payables")
@RequiredArgsConstructor
public class PayableController {
    private final PayableService payableService;
    private final SystemAuthorizationService authorizationService;

    private TokenStore.LoginUser currentUser(HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }

    @GetMapping
    public Result<PageResult<PayableDtos.ListResponse>> list(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueStartDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueEndDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            HttpServletRequest request) {
        authorizationService.requireFinanceAccess(currentUser(request));
        return Result.ok(payableService.list(supplierId, status, startDate, endDate,
                dueStartDate, dueEndDate, page, size));
    }

    @GetMapping("/{id}")
    public Result<PayableDtos.ListResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        authorizationService.requireFinanceAccess(currentUser(request));
        return Result.ok(payableService.detail(id));
    }

    @GetMapping("/aging")
    public Result<List<PayableDtos.AgingResponse>> aging(@RequestParam(required = false) Long supplierId,
                                                         HttpServletRequest request) {
        authorizationService.requireFinanceAccess(currentUser(request));
        return Result.ok(payableService.aging(supplierId));
    }
}
