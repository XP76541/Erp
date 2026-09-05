package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.service.StatementAdjustmentService;
import com.erp.module.finance.service.ReportExcelService;
import com.erp.module.system.TokenStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 对账单调整Controller
 */
@RestController
@RequestMapping("/finance/statement-adjustments")
@RequiredArgsConstructor
public class StatementAdjustmentController {

    private final StatementAdjustmentService adjustmentService;
    private final ReportExcelService reportExcelService;

    /**
     * 创建对账单调整
     */
    @PostMapping
    public Result<Long> create(@jakarta.validation.Valid @RequestBody ReceivableDtos.StatementAdjustmentRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long id = adjustmentService.createAdjustment(request, currentUser);
        return Result.ok(id);
    }

    /**
     * 获取指定客户的调整记录
     */
    @GetMapping("/customer/{customerId}")
    public Result<List<ReceivableDtos.StatementAdjustmentResponse>> getAdjustmentsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        List<ReceivableDtos.StatementAdjustmentResponse> adjustments = adjustmentService.getAdjustmentsByCustomer(customerId, start, end, TokenStore.getCurrentLoginUser());
        return Result.ok(adjustments);
    }

    /**
     * 获取指定对账单的调整记录
     */
    @GetMapping("/statement/{statementId}")
    public Result<List<ReceivableDtos.StatementAdjustmentResponse>> getAdjustmentsByStatement(@PathVariable Long statementId) {
        List<ReceivableDtos.StatementAdjustmentResponse> adjustments = adjustmentService.getAdjustmentsByStatement(
                statementId, TokenStore.getCurrentLoginUser());
        return Result.ok(adjustments);
    }

    /**
     * 更新调整记录
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody ReceivableDtos.StatementAdjustmentRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        adjustmentService.updateAdjustment(id, request, currentUser);
        return Result.ok();
    }

    /**
     * 删除调整记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        adjustmentService.deleteAdjustment(id, currentUser);
        return Result.ok();
    }

    /**
     * 获取调整统计
     */
    @GetMapping("/statistics")
    public Result<List<ReceivableDtos.AdjustmentStatisticsResponse>> getAdjustmentStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        List<ReceivableDtos.AdjustmentStatisticsResponse> statistics = adjustmentService.getAdjustmentStatistics(start, end,
                TokenStore.getCurrentLoginUser());
        return Result.ok(statistics);
    }

    /**
     * 生成包含调整的对账单
     */
    @GetMapping("/statement-with-adjustments")
    public Result<ReceivableDtos.StatementResponse> generateStatementWithAdjustments(
            @RequestParam Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        ReceivableDtos.StatementResponse statement = adjustmentService.generateStatementWithAdjustments(
                customerId,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                TokenStore.getCurrentLoginUser());
        return Result.ok(statement);
    }

    /**
     * 导出对账单
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportStatement(
            @RequestParam Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        ReceivableDtos.StatementResponse statement = adjustmentService.generateStatementWithAdjustments(
                customerId, start, end, TokenStore.getCurrentLoginUser());
        byte[] bytes = reportExcelService.statement(statement);
        String fileName = URLEncoder.encode("客户对账单_" + customerId + "_" + end + ".xlsx",
                StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }

    /**
     * 验证对账单平衡
     */
    @GetMapping("/validate-balance")
    public Result<Boolean> validateBalance(
            @RequestParam Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        // 生成对账单并验证平衡
        ReceivableDtos.StatementResponse statement = adjustmentService.generateStatementWithAdjustments(
                customerId,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                TokenStore.getCurrentLoginUser());

        // 验证平衡：期初 + 本期应收 - 本期收款 - 调整 = 期末
        boolean isBalanced = statement.getClosingBalance().compareTo(
                statement.getOpeningBalance()
                        .add(statement.getCurrentReceivables())
                        .subtract(statement.getPayments())
                        .subtract(statement.getAdjustments())) == 0;

        return Result.ok(isBalanced);
    }

    /**
     * 获取对账单摘要
     */
    @GetMapping("/summary")
    public Result<StatementSummary> getStatementSummary(
            @RequestParam Long customerId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        ReceivableDtos.StatementResponse statement = adjustmentService.generateStatementWithAdjustments(
                customerId,
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                TokenStore.getCurrentLoginUser());

        StatementSummary summary = new StatementSummary();
        summary.setCustomerId(customerId);
        summary.setCustomerName(statement.getCustomerName());
        summary.setStartDate(LocalDate.parse(startDate));
        summary.setEndDate(LocalDate.parse(endDate));
        summary.setOpeningBalance(statement.getOpeningBalance());
        summary.setCurrentReceivables(statement.getCurrentReceivables());
        summary.setPayments(statement.getPayments());
        summary.setAdjustments(statement.getAdjustments());
        summary.setClosingBalance(statement.getClosingBalance());
        summary.setDetailCount(statement.getDetails() != null ? statement.getDetails().size() : 0);

        return Result.ok(summary);
    }

    /**
     * 对账单摘要内部类
     */
    public static class StatementSummary {
        private Long customerId;
        private String customerName;
        private LocalDate startDate;
        private LocalDate endDate;
        private java.math.BigDecimal openingBalance;
        private java.math.BigDecimal currentReceivables;
        private java.math.BigDecimal payments;
        private java.math.BigDecimal adjustments;
        private java.math.BigDecimal closingBalance;
        private Integer detailCount;

        // Getters and Setters
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public java.math.BigDecimal getOpeningBalance() { return openingBalance; }
        public void setOpeningBalance(java.math.BigDecimal openingBalance) { this.openingBalance = openingBalance; }
        public java.math.BigDecimal getCurrentReceivables() { return currentReceivables; }
        public void setCurrentReceivables(java.math.BigDecimal currentReceivables) { this.currentReceivables = currentReceivables; }
        public java.math.BigDecimal getPayments() { return payments; }
        public void setPayments(java.math.BigDecimal payments) { this.payments = payments; }
        public java.math.BigDecimal getAdjustments() { return adjustments; }
        public void setAdjustments(java.math.BigDecimal adjustments) { this.adjustments = adjustments; }
        public java.math.BigDecimal getClosingBalance() { return closingBalance; }
        public void setClosingBalance(java.math.BigDecimal closingBalance) { this.closingBalance = closingBalance; }
        public Integer getDetailCount() { return detailCount; }
        public void setDetailCount(Integer detailCount) { this.detailCount = detailCount; }
    }
}