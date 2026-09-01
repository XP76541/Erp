package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReceivableStatisticsResponse {
    private Long customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private BigDecimal unsettledAmount;
    private BigDecimal partialAmount;
    private BigDecimal settledAmount;
}