package com.erp.module.finance.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayableDtos {
    @Data
    public static class ListResponse {
        private Long id;
        private Long supplierId;
        private String supplierName;
        private String docType;
        private Long docId;
        private String docNo;
        private LocalDate bizDate;
        private LocalDate dueDate;
        private BigDecimal amount;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private String status;
        private Integer daysOverdue;
        private String agingBucket;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AgingResponse {
        private String bucket;
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal paidAmount = BigDecimal.ZERO;
        private BigDecimal remainingAmount = BigDecimal.ZERO;
        private Integer count = 0;
    }
}
