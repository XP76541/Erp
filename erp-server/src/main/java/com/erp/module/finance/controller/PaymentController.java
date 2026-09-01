package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.system.entity.SysUser;
import com.erp.module.system.TokenStore;
import com.erp.module.finance.service.PaymentService;
import com.erp.module.finance.dto.PaymentDtos;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/finance/payments")
public class PaymentController {

    @Resource
    private PaymentService paymentService;

    @PostMapping
    public Result<Long> create(@RequestBody PaymentDtos.PaymentCreateRequest request) {
        SysUser currentUser = TokenStore.getCurrentLoginUser();
        Payment payment = paymentService.createDraft(request, currentUser);
        return Result.success(payment.getId());
    }

    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @RequestBody PaymentDtos.PaymentAuditRequest request) {
        SysUser currentUser = TokenStore.getCurrentLoginUser();
        paymentService.audit(id, request, currentUser);
        return Result.success();
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
        return Result.success(result);
    }

    @GetMapping("/receivables/{customerId}")
    public Result<List<PaymentDtos.ReceivableListResponse>> getReceivablesByCustomer(@PathVariable Long customerId) {
        List<PaymentDtos.ReceivableListResponse> receivables = paymentService.getReceivablesByCustomer(customerId);
        return Result.success(receivables);
    }

    @GetMapping("/statistics")
    public Result<List<PaymentDtos.ReceivableStatisticsResponse>> getStatistics() {
        List<PaymentDtos.ReceivableStatisticsResponse> statistics = paymentService.getCustomerStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/aging-analysis")
    public Result<List<PaymentDtos.AgingAnalysisResponse>> getAgingAnalysis() {
        List<PaymentDtos.AgingAnalysisResponse> analysis = paymentService.getAgingAnalysis();
        return Result.success(analysis);
    }

    @GetMapping("/overdue")
    public Result<List<PaymentDtos.ReceivableListResponse>> getOverdueReceivables() {
        List<PaymentDtos.ReceivableListResponse> overdue = paymentService.getOverdueReceivables();
        return Result.success(overdue);
    }
}