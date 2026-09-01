package com.erp.module.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SalesDailyReportRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long customerId;
    private Long salespersonId;
}