package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.module.finance.service.ReportService;
import com.erp.module.finance.dto.*;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/finance/reports")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 销售日报表
     */
    @PostMapping("/sales-daily")
    public Result<List<ReportDtos.SalesDailyReportResponse>> getSalesDailyReport(@RequestBody ReportDtos.SalesDailyReportRequest request) {
        List<ReportDtos.SalesDailyReportResponse> result = reportService.getSalesDailyReport(request);
        return Result.success(result);
    }

    /**
     * 进销存汇总报表
     */
    @PostMapping("/inventory-summary")
    public Result<ReportDtos.InventorySummaryResponse> getInventorySummary(@RequestBody ReportDtos.InventorySummaryRequest request) {
        ReportDtos.InventorySummaryResponse result = reportService.getInventorySummary(request);
        return Result.success(result);
    }

    /**
     * 财务汇总报表
     */
    @PostMapping("/finance-summary")
    public Result<ReportDtos.FinanceSummaryResponse> getFinanceSummary(@RequestBody ReportDtos.FinanceSummaryRequest request) {
        ReportDtos.FinanceSummaryResponse result = reportService.getFinanceSummary(request);
        return Result.success(result);
    }

    /**
     * 导出销售日报表
     */
    @GetMapping("/sales-daily/export")
    public Result<String> exportSalesDailyReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long salespersonId) {

        ReportDtos.SalesDailyReportRequest request = new ReportDtos.SalesDailyReportRequest();
        if (startDate != null) request.setStartDate(java.time.LocalDate.parse(startDate));
        if (endDate != null) request.setEndDate(java.time.LocalDate.parse(endDate));
        request.setCustomerId(customerId);
        request.setSalespersonId(salespersonId);

        List<ReportDtos.SalesDailyReportResponse> data = reportService.getSalesDailyReport(request);

        // TODO: 实现Excel导出功能
        String fileName = "销售日报表_" + java.time.LocalDate.now() + ".xlsx";
        return Result.success(fileName);
    }

    /**
     * 导出进销存汇总报表
     */
    @GetMapping("/inventory-summary/export")
    public Result<String> exportInventorySummary(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long productId) {

        ReportDtos.InventorySummaryRequest request = new ReportDtos.InventorySummaryRequest();
        if (date != null) request.setDate(java.time.LocalDate.parse(date));
        request.setWarehouseId(warehouseId);
        request.setProductId(productId);

        ReportDtos.InventorySummaryResponse data = reportService.getInventorySummary(request);

        // TODO: 实现Excel导出功能
        String fileName = "进销存汇总表_" + java.time.LocalDate.now() + ".xlsx";
        return Result.success(fileName);
    }

    /**
     * 导出财务汇总报表
     */
    @GetMapping("/finance-summary/export")
    public Result<String> exportFinanceSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        ReportDtos.FinanceSummaryRequest request = new ReportDtos.FinanceSummaryRequest();
        if (startDate != null) request.setStartDate(java.time.LocalDate.parse(startDate));
        if (endDate != null) request.setEndDate(java.time.LocalDate.parse(endDate));

        ReportDtos.FinanceSummaryResponse data = reportService.getFinanceSummary(request);

        // TODO: 实现Excel导出功能
        String fileName = "财务汇总表_" + java.time.LocalDate.now() + ".xlsx";
        return Result.success(fileName);
    }
}