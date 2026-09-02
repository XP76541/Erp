package com.erp.module.finance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 供应商付款及应付核销 DTO */
public class PaymentDtos {
    @Data
    public static class PaymentCreateRequest {
        @NotNull(message = "供应商不能为空")
        private Long supplierId;
        private LocalDate bizDate;
        @NotNull(message = "付款金额不能为空")
        @DecimalMin(value = "0.01", message = "付款金额必须大于0")
        private BigDecimal amount;
        private String method;
        private String bankAccount;
        private String remark;
        @Valid
        private List<AllocationItem> allocations;

        @Data
        public static class AllocationItem {
            @NotNull(message = "应付账款不能为空")
            private Long payableId;
            @NotNull(message = "核销金额不能为空")
            @DecimalMin(value = "0.01", message = "核销金额必须大于0")
            private BigDecimal amount;
        }
    }

    @Data
    public static class PaymentListRequest {
        private Long supplierId;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class PaymentListResponse {
        private Long id;
        private String docNo;
        private Long supplierId;
        private String supplierName;
        private LocalDate bizDate;
        private BigDecimal amount;
        private BigDecimal allocatedAmount;
        private String status;
        private String method;
        private String bankAccount;
        private String remark;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PaymentAuditRequest {
        private String remark;
        private String ip;
    }
}
