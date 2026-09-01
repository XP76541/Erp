package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceSummaryResponse {
    private LocalDate reportDate;
    private BigDecimal totalSales;
    private BigDecimal totalPurchases;
    private BigDecimal totalReceivables;
    private BigDecimal totalPayables;
    private BigDecimal totalInventory;
    private BigDecimal netProfit;
}