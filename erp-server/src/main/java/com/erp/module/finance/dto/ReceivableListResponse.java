package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ReceivableListResponse {
    private Long id;
    private String docNo;
    private String orderDocNo;
    private Long customerId;
    private String customerName;
    private LocalDate businessDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String status;
    private Integer daysOverdue;
    private String agingBucket;
    private String createdAt;
}