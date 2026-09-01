package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.erp.module.finance.service.ReceivableService;
import com.erp.module.finance.dto.ReceivableDtos;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/finance/receivables")
public class ReceivableController {

    @Resource
    private ReceivableService receivableService;

    @GetMapping
    public Result<PageResult<ReceivableDtos.ReceivableListResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        ReceivableDtos.ReceivableListRequest params = new ReceivableDtos.ReceivableListRequest();
        params.setCustomerId(customerId);
        params.setStatus(status);
        if (startDate != null) params.setStartDate(java.time.LocalDate.parse(startDate));
        if (endDate != null) params.setEndDate(java.time.LocalDate.parse(endDate));
        params.setPage(page);
        params.setSize(size);

        PageResult<ReceivableDtos.ReceivableListResponse> result = receivableService.getReceivables(params);
        return Result.success(result);
    }

    @GetMapping("/statistics")
    public Result<List<ReceivableDtos.ReceivableStatisticsResponse>> getStatistics() {
        List<ReceivableDtos.ReceivableStatisticsResponse> statistics = receivableService.getCustomerStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/aging-analysis")
    public Result<List<ReceivableDtos.AgingAnalysisResponse>> getAgingAnalysis() {
        List<ReceivableDtos.AgingAnalysisResponse> analysis = receivableService.getAgingAnalysis();
        return Result.success(analysis);
    }

    @GetMapping("/overdue")
    public Result<List<ReceivableDtos.ReceivableListResponse>> getOverdueReceivables() {
        List<ReceivableDtos.ReceivableListResponse> overdue = receivableService.getOverdueReceivables();
        return Result.success(overdue);
    }
}