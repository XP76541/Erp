package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.TokenStore;
import com.erp.module.finance.service.PaymentService;
import com.erp.module.finance.service.ReceivableService;
import com.erp.module.finance.dto.PaymentDtos;
import com.erp.module.finance.dto.ReceivableDtos;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import com.erp.module.finance.entity.Payment;
import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/finance/payments")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @Resource
    private ReceivableService receivableService;

    @PostMapping
    public Result<Long> create(@RequestBody PaymentDtos.PaymentCreateRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Payment payment = paymentService.createDraft(request, currentUser);
        return Result.ok(payment.getId());
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody PaymentDtos.PaymentAuditRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        HttpServletRequest req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String ip = req.getRemoteAddr();
        paymentService.audit(id, currentUser, ip);
        return Result.ok();
    }

    @GetMapping
    public Result<PageResult<PaymentDtos.PaymentListResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        PaymentDtos.PaymentListRequest params = new PaymentDtos.PaymentListRequest();
        params.setCustomerId(customerId);
        params.setStatus(status);
        params.setPage(page);
        params.setSize(size);

        PageResult<PaymentDtos.PaymentListResponse> result = paymentService.getPayments(params);
        return Result.ok(result);
    }

    @GetMapping("/receivables/{customerId}")
    public Result<List<PaymentDtos.ReceivableListResponse>> getReceivablesByCustomer(@PathVariable Long customerId) {
        List<PaymentDtos.ReceivableListResponse> receivables = paymentService.getReceivablesByCustomer(customerId);
        return Result.ok(receivables);
    }

    @GetMapping("/statistics")
    public Result<List<ReceivableDtos.ReceivableStatisticsResponse>> getStatistics() {
        List<ReceivableDtos.ReceivableStatisticsResponse> statistics = receivableService.getCustomerStatistics();
        return Result.ok(statistics);
    }

    @GetMapping("/aging-analysis")
    public Result<List<ReceivableDtos.AgingAnalysisResponse>> getAgingAnalysis() {
        List<ReceivableDtos.AgingAnalysisResponse> analysis = receivableService.getAgingAnalysis();
        return Result.ok(analysis);
    }

    @GetMapping("/overdue")
    public Result<List<ReceivableDtos.ReceivableListResponse>> getOverdueReceivables() {
        List<ReceivableDtos.ReceivableListResponse> overdue = receivableService.getOverdueReceivables();
        return Result.ok(overdue);
    }
}