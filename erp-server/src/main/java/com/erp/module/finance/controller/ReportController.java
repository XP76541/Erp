package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.module.finance.service.ReportService;
import com.erp.module.finance.service.ReportExcelService;
import com.erp.module.finance.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.nio.charset.StandardCharsets;
import com.erp.module.system.AuthInterceptor;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.SystemAuthorizationService;

@RestController
@RequestMapping("/finance/reports")
public class ReportController {

    @Resource
    private ReportService reportService;
    @Resource
    private ReportExcelService excelService;
    @Resource
    private SystemAuthorizationService authorizationService;

    private TokenStore.LoginUser currentUser(jakarta.servlet.http.HttpServletRequest request) {
        return (TokenStore.LoginUser) request.getAttribute(AuthInterceptor.ATTR_LOGIN_USER);
    }

    /**
     * 销售日报表
     */
    @PostMapping("/sales-daily")
    public Result<List<ReportDtos.SalesDailyReportResponse>> getSalesDailyReport(@RequestBody ReportDtos.SalesDailyReportRequest request,
                                                                                  jakarta.servlet.http.HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        authorizationService.requireReportAccess(user);
        Long scope = authorizationService.reportSalespersonScope(user);
        if (scope != null) request.setSalespersonId(scope);
        List<ReportDtos.SalesDailyReportResponse> result = reportService.getSalesDailyReport(request);
        return Result.success(result);
    }

    /**
     * 进销存汇总报表
     */
    @PostMapping("/inventory-summary")
    public Result<ReportDtos.InventorySummaryResponse> getInventorySummary(@RequestBody ReportDtos.InventorySummaryRequest request,
                                                                             jakarta.servlet.http.HttpServletRequest httpRequest) {
        authorizationService.requireReportAccess(currentUser(httpRequest));
        ReportDtos.InventorySummaryResponse result = reportService.getInventorySummary(request);
        return Result.success(result);
    }

    /**
     * 财务汇总报表
     */
    @PostMapping("/finance-summary")
    public Result<ReportDtos.FinanceSummaryResponse> getFinanceSummary(@RequestBody ReportDtos.FinanceSummaryRequest request,
                                                                         jakarta.servlet.http.HttpServletRequest httpRequest) {
        authorizationService.requireReportAccess(currentUser(httpRequest));
        ReportDtos.FinanceSummaryResponse result = reportService.getFinanceSummary(request);
        return Result.success(result);
    }

    /**
     * 导出销售日报表
     */
    @GetMapping("/sales-daily/export")
    public ResponseEntity<byte[]> exportSalesDailyReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        TokenStore.LoginUser user = currentUser(httpRequest);
        authorizationService.requireReportAccess(user);
        Long scope = authorizationService.reportSalespersonScope(user);
        if (scope != null) salespersonId = scope;
        ReportDtos.SalesDailyReportRequest request = new ReportDtos.SalesDailyReportRequest();
        if (startDate != null) request.setStartDate(java.time.LocalDate.parse(startDate));
        if (endDate != null) request.setEndDate(java.time.LocalDate.parse(endDate));
        request.setCustomerId(customerId);
        request.setSalespersonId(salespersonId);

        List<ReportDtos.SalesDailyReportResponse> data = reportService.getSalesDailyReport(request);

        byte[] bytes = excelService.salesDaily(data);
        return download(bytes, "销售日报表_" + java.time.LocalDate.now() + ".xlsx");
    }

    /**
     * 导出进销存汇总报表
     */
    @GetMapping("/inventory-summary/export")
    public ResponseEntity<byte[]> exportInventorySummary(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        authorizationService.requireReportAccess(currentUser(httpRequest));
        ReportDtos.InventorySummaryRequest request = new ReportDtos.InventorySummaryRequest();
        if (date != null) request.setDate(java.time.LocalDate.parse(date));
        request.setWarehouseId(warehouseId);
        request.setProductId(productId);

        ReportDtos.InventorySummaryResponse data = reportService.getInventorySummary(request);

        byte[] bytes = excelService.inventorySummary(data);
        return download(bytes, "进销存汇总表_" + java.time.LocalDate.now() + ".xlsx");
    }

    /**
     * 导出财务汇总报表
     */
    @GetMapping("/finance-summary/export")
    public ResponseEntity<byte[]> exportFinanceSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        authorizationService.requireReportAccess(currentUser(httpRequest));
        ReportDtos.FinanceSummaryRequest request = new ReportDtos.FinanceSummaryRequest();
        if (startDate != null) request.setStartDate(java.time.LocalDate.parse(startDate));
        if (endDate != null) request.setEndDate(java.time.LocalDate.parse(endDate));

        ReportDtos.FinanceSummaryResponse data = reportService.getFinanceSummary(request);

        byte[] bytes = excelService.financeSummary(data);
        return download(bytes, "财务汇总表_" + java.time.LocalDate.now() + ".xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String fileName) {
        String encoded = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(bytes);
    }
}