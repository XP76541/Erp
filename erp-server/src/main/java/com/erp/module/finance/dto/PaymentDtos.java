package com.erp.module.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收款相关的DTO
 */
public class PaymentDtos {

    @Data
    public static class PaymentCreateRequest {
        @NotNull
        private Long customerId;
        private LocalDate businessDate;
        @DecimalMin("0.01")
        private BigDecimal amount;
        private String paymentMethod;
        private String remark;
        private List<AllocationItem> allocations;

        @Data
        public static class AllocationItem {
            @NotNull
            private Long receivableId;
            @DecimalMin("0.01")
            private BigDecimal allocatedAmount;
            private String receivableDocNo;
            private BigDecimal receivableAmount;
            private BigDecimal remainingAmount;
        }
    }

    @Data
    public static class PaymentListRequest {
        private Long customerId;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer page;
        private Integer size;
    }

    @Data
    public static class PaymentListResponse {
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

    @Data
    public static class PaymentAuditRequest {
        private String action;
        private String remark;
        private String ip;
    }

    @Data
    public static class PaymentAllocationResponse {
        private Long paymentId;
        private String paymentDocNo;
        private Long receivableId;
        private String receivableDocNo;
        private BigDecimal allocatedAmount;
        private LocalDate createdAt;
    }

    @Data
    public static class ReceivableListResponse {
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

    @Data
    public static class ReceivableStatisticsResponse {
        private Long customerId;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
        private BigDecimal unsettledAmount;
        private BigDecimal partialAmount;
        private BigDecimal settledAmount;
    }

    @Data
    public static class AgingAnalysisResponse {
        private String agingBucket;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
    }
}
