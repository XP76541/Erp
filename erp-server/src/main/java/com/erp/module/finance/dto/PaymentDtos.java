package com.erp.module.finance.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PaymentCreateRequest {
    @NotNull
    private Long customerId;
    private LocalDate businessDate;
    @DecimalMin("0.01")
    private BigDecimal amount;
    private String paymentMethod;
    private String remark;

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

    private List<AllocationItem> allocations;
}

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

@Data
public class PaymentAuditRequest {
    private String action; // audit or reject
    private String remark;
    private String ip;
}

@Data
public class PaymentAllocationResponse {
    private Long paymentId;
    private String paymentDocNo;
    private Long receivableId;
    private String receivableDocNo;
    private BigDecimal allocatedAmount;
    private LocalDate createdAt;
}