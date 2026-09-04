package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.service.ReceivableService;
import com.erp.module.finance.service.ReceiptService;
import com.erp.module.system.service.SystemAuthorizationService;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.system.TokenStore;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收账款Controller
 */
@RestController
@RequestMapping("/finance/receivables")
@RequiredArgsConstructor
public class ReceivableController {

    private final ReceivableService receivableService;
    private final ReceiptService receiptService;
    private final SystemAuthorizationService authorizationService;

    private TokenStore.LoginUser currentUser() {
        return TokenStore.getCurrentLoginUser();
    }

    /**
     * 分页查询应收账款
     */
    @GetMapping
    public Result<PageResult<ReceivableDtos.ReceivableListResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long size) {

        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        request.setCustomerId(customerId);
        request.setStatus(status);
        request.setStartDate(startDate != null ? java.time.LocalDate.parse(startDate) : null);
        request.setEndDate(endDate != null ? java.time.LocalDate.parse(endDate) : null);

        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        return Result.success(result);
    }

    /**
     * 获取应收账款详情
     */
    @GetMapping("/{id}")
    public Result<ReceivableDtos.ReceivableListResponse> detail(@PathVariable Long id) {
        ReceivableDtos.ReceivableListResponse detail = receivableService.getReceivableDetail(id, currentUser());
        return Result.success(detail);
    }

    /**
     * 创建应收账款
     */
    @PostMapping
    public Result<Long> create(@RequestBody ReceivableDtos.ReceivableCreateRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long count = receivableService.createReceivable(request, currentUser);
        return Result.success(count);
    }

    /** 客户收款多单核销 */
    @PostMapping("/receipts")
    public Result<ReceivableDtos.ReceiptResponse> createReceipt(@Valid @RequestBody ReceivableDtos.ReceiptCreateRequest request) {
        authorizationService.requireFinanceAccess(currentUser());
        return Result.success(receiptService.createAndAllocate(request, currentUser()));
    }

    @PostMapping("/{id}/settle")
    public Result<ReceivableDtos.SettleResponse> settle(@PathVariable Long id,
                                                      @RequestBody ReceivableDtos.SettleRequest request) {
        request.setReceivableId(id);
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        ReceivableDtos.SettleResponse response = receivableService.settleReceivable(request, currentUser);
        return Result.success(response);
    }

    /**
     * 批量核销
     */
    @PostMapping("/batch-settle")
    public Result<Void> batchSettle(@RequestBody ReceivableDtos.BatchSettleRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        receivableService.batchSettle(request, currentUser);
        return Result.success();
    }

    /**
     * 获取客户应收账款统计
     */
    @GetMapping("/statistics")
    public Result<List<ReceivableDtos.ReceivableStatisticsResponse>> getStatistics() {
        List<ReceivableDtos.ReceivableStatisticsResponse> statistics = receivableService.getCustomerStatistics(currentUser());
        return Result.success(statistics);
    }

    /**
     * 获取客户应收账款汇总
     */
    @GetMapping("/customer/{customerId}/summary")
    public Result<ReceivableDtos.CustomerReceivableSummary> getCustomerSummary(@PathVariable Long customerId) {
        ReceivableDtos.CustomerReceivableSummary summary = receivableService.getCustomerSummary(customerId, currentUser());
        return Result.success(summary);
    }

    /**
     * 获取账龄分析
     */
    @GetMapping("/aging-analysis")
    public Result<List<ReceivableDtos.AgingAnalysisResponse>> getAgingAnalysis(
            @RequestParam(required = false) String cutoffDate) {
        LocalDate cutoff = cutoffDate == null ? LocalDate.now() : LocalDate.parse(cutoffDate);
        List<ReceivableDtos.AgingAnalysisResponse> analysis = receivableService.getAgingAnalysis(cutoff, currentUser());
        return Result.success(analysis);
    }

    /**
     * 获取逾期应收账款
     */
    @GetMapping("/overdue")
    public Result<List<ReceivableDtos.ReceivableListResponse>> getOverdueReceivables(
            @RequestParam(required = false) String cutoffDate) {
        LocalDate cutoff = cutoffDate == null ? LocalDate.now() : LocalDate.parse(cutoffDate);
        List<ReceivableDtos.ReceivableListResponse> overdue = receivableService.getOverdueReceivables(cutoff, currentUser());
        return Result.success(overdue);
    }

    /**
     * 生成对账单
     */
    @GetMapping("/statement")
    public Result<ReceivableDtos.StatementResponse> generateStatement(
            @RequestParam Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        ReceivableDtos.StatementResponse statement = receivableService.generateStatement(
                customerId,
                java.time.LocalDate.parse(startDate),
                java.time.LocalDate.parse(endDate),
                currentUser());
        return Result.success(statement);
    }

    /**
     * 获取未结算的应收账款数量
     */
    @GetMapping("/stats/unsettled-count")
    public Result<Integer> getUnsettledCount() {
        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        request.setStatus("UNSETTLED");
        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        return Result.success((int) result.getTotal());
    }

    /**
     * 获取已结算的应收账款数量
     */
    @GetMapping("/stats/settled-count")
    public Result<Integer> getSettledCount() {
        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        request.setStatus("SETTLED");
        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        return Result.success((int) result.getTotal());
    }

    /**
     * 获取逾期应收账款数量
     */
    @GetMapping("/stats/overdue-count")
    public Result<Integer> getOverdueCount() {
        List<ReceivableDtos.ReceivableListResponse> overdue = receivableService.getOverdueReceivables(LocalDate.now(), currentUser());
        return Result.success(overdue.size());
    }

    /**
     * 获取应收账款总金额
     */
    @GetMapping("/stats/total-amount")
    public Result<BigDecimal> getTotalAmount() {
        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        BigDecimal totalAmount = result.getRecords().stream()
                .map(ReceivableDtos.ReceivableListResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.success(totalAmount);
    }

    /**
     * 获取已收款项总金额
     */
    @GetMapping("/stats/total-paid")
    public Result<BigDecimal> getTotalPaid() {
        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        BigDecimal totalPaid = result.getRecords().stream()
                .map(ReceivableDtos.ReceivableListResponse::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.success(totalPaid);
    }

    /**
     * 获取剩余应收款总金额
     */
    @GetMapping("/stats/total-remaining")
    public Result<BigDecimal> getTotalRemaining() {
        ReceivableDtos.ReceivableListRequest request = new ReceivableDtos.ReceivableListRequest();
        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(request, currentUser());
        BigDecimal totalRemaining = result.getRecords().stream()
                .map(ReceivableDtos.ReceivableListResponse::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Result.success(totalRemaining);
    }
}