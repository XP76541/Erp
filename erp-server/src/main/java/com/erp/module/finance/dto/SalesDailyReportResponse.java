package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesDailyReportResponse {
    private LocalDate reportDate;
    private Long totalOrders;
    private BigDecimal totalAmount;
    private BigDecimal shippedAmount;
    private List<SalesDailyReportItem> orders;
}