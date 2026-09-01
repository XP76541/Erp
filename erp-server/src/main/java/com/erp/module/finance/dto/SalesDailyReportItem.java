package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalesDailyReportItem {
    private String docNo;
    private LocalDate businessDate;
    private String customerName;
    private String salespersonName;
    private BigDecimal amount;
    private BigDecimal shippedAmount;
    private String status;
}