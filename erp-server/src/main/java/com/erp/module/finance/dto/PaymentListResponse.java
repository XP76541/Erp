package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentListResponse {
    private Long id;
    private String docNo;
    private Long customerId;
    private String customerName;
    private LocalDate businessDate;
    private BigDecimal amount;
    private BigDecimal allocatedAmount;
    private String status;
    private String paymentMethod;
    private String createdAt;
}