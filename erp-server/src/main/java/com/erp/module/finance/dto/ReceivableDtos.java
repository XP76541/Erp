package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应收账款相关的DTO
 */
public class ReceivableDtos {

    /**
     * 应收账款列表请求
     */
    @Data
    public static class ReceivableListRequest {
        private Long customerId;
        private String status;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    /**
     * 应收账款列表响应
     */
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

    /**
     * 创建应收账款请求
     */
    @Data
    public static class ReceivableCreateRequest {
        private Long customerId;
        private LocalDate businessDate;
        private String remark;

        @Data
        public static class Item {
            private Long orderId;
            private BigDecimal amount;
        }

        private List<Item> items;
    }

    /**
     * 应收账款统计响应
     */
    @Data
    public static class ReceivableStatisticsResponse {
        private Long customerId;
        private String customerName;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
        private BigDecimal unsettledAmount;
        private BigDecimal partialAmount;
        private BigDecimal settledAmount;

        public ReceivableStatisticsResponse(Long customerId, String customerName,
                                          BigDecimal totalAmount, BigDecimal totalPaid,
                                          BigDecimal totalRemaining, BigDecimal unsettledAmount,
                                          BigDecimal partialAmount, BigDecimal settledAmount) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.totalPaid = totalPaid;
            this.totalRemaining = totalRemaining;
            this.unsettledAmount = unsettledAmount;
            this.partialAmount = partialAmount;
            this.settledAmount = settledAmount;
        }
    }

    /**
     * 账龄分析响应
     */
    @Data
    public static class AgingAnalysisResponse {
        private String agingBucket;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;

        public AgingAnalysisResponse(String agingBucket, BigDecimal totalAmount,
                                    BigDecimal totalPaid, BigDecimal totalRemaining) {
            this.agingBucket = agingBucket;
            this.totalAmount = totalAmount;
            this.totalPaid = totalPaid;
            this.totalRemaining = totalRemaining;
        }
    }

    /**
     * 客户应收账款汇总
     */
    @Data
    public static class CustomerReceivableSummary {
        private Long customerId;
        private String customerName;
        private BigDecimal totalReceivable;
        private BigDecimal totalPaid;
        private BigDecimal totalOverdue;
        private Integer countUnsettled;
        private Integer countOverdue;
        private List<AgingDistribution> agingDistribution;

        @Data
        public static class AgingDistribution {
            private String bucket;
            private BigDecimal amount;
            private Integer count;
        }
    }

    /**
     * 收款核销请求
     */
    @Data
    public static class SettleRequest {
        private Long receivableId;
        private BigDecimal amount;
        private String paymentMethod;
        private String remark;
    }

    /**
     * 批量核销请求
     */
    @Data
    public static class BatchSettleRequest {
        private List<SettleRequest> settlements;
        private String batchNo;
    }

    /**
     * 核销响应
     */
    @Data
    public static class SettleResponse {
        private Long paymentId;
        private String paymentNo;
        private BigDecimal settledAmount;
        private String status;
        private String message;
    }

    /**
     * 催收记录
     */
    @Data
    public static class CollectionRecord {
        private Long id;
        private Long receivableId;
        private Long customerId;
        private String customerName;
        private BigDecimal amount;
        private String contactMethod;
        private String contactResult;
        private String nextAction;
        private String operator;
        private LocalDateTime createdAt;

        public CollectionRecord(Receivable receivable) {
            this.receivableId = receivable.getId();
            this.customerId = receivable.getCustomerId();
            this.customerName = receivable.getCustomerName();
            this.amount = receivable.getRemainingAmount();
            this.createdAt = LocalDateTime.now();
        }
    }

    /**
     * 催收请求
     */
    @Data
    public static class CollectionRequest {
        private Long receivableId;
        private String contactMethod;
        private String contactResult;
        private String nextAction;
        private String remark;
    }

    /**
     * 应收账款催收响应
     */
    @Data
    public static class CollectionResponse {
        private Long id;
        private String receivableDocNo;
        private Long customerId;
        private String customerName;
        private BigDecimal amount;
        private Integer daysOverdue;
        private String contactMethod;
        private String contactResult;
        private String nextAction;
        private String operator;
        private LocalDateTime createdAt;
    }

    /**
     * 催收统计响应
     */
    @Data
    public static class CollectionStatsResponse {
        private Integer totalOverdueCount;
        private BigDecimal totalOverdueAmount;
        private Integer contactedCount;
        private Integer promisedPaymentCount;
        private Integer disputedCount;
        private List<CollectionStatusCount> statusCounts;

        @Data
        public static class CollectionStatusCount {
            private String status;
            private Integer count;
            private BigDecimal amount;
        }
    }

    /**
     * 生成对账单请求
     */
    @Data
    public static class GenerateStatementRequest {
        private Long customerId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String templateType;
    }

    /**
     * 对账单响应
     */
    @Data
    public static class StatementResponse {
        private Long customerId;
        private String customerName;
        private LocalDate statementDate;
        private BigDecimal openingBalance;
        private BigDecimal currentReceivables;
        private BigDecimal payments;
        private BigDecimal adjustments;
        private BigDecimal closingBalance;
        private List<StatementDetail> details;

        @Data
        public static class StatementDetail {
            private LocalDate date;
            private String docNo;
            private String docType;
            private BigDecimal amount;
            private BigDecimal paid;
            private BigDecimal remaining;
            private String status;
            private String remark;
        }
    }

    /**
     * 对账单调整请求
     */
    @Data
    public static class StatementAdjustmentRequest {
        private Long statementId;
        private Long customerId;
        private LocalDate adjustmentDate;
        private BigDecimal adjustmentAmount;
        private String adjustmentType;
        private String reason;
        private String remark;
    }

    /**
     * 对账单调整响应
     */
    @Data
    public static class StatementAdjustmentResponse {
        private Long id;
        private Long customerId;
        private String customerName;
        private LocalDate adjustmentDate;
        private BigDecimal adjustmentAmount;
        private String adjustmentType;
        private String reason;
        private String remark;
        private String operator;
        private String createdAt;
    }

    /**
     * 调整统计响应
     */
    @Data
    public static class AdjustmentStatisticsResponse {
        private String adjustmentType;
        private Integer count;
        private BigDecimal totalAmount;

        public AdjustmentStatisticsResponse(String adjustmentType, Integer count, BigDecimal totalAmount) {
            this.adjustmentType = adjustmentType;
            this.count = count;
            this.totalAmount = totalAmount;
        }
    }

    /**
     * 异常请求
     */
    @Data
    public static class ExceptionRequest {
        private Long receivableId;
        private String exceptionType;
        private String exceptionLevel;
        private String description;
        private String impact;
        private String suggestedAction;
    }

    /**
     * 异常解决请求
     */
    @Data
    public static class ExceptionResolutionRequest {
        private String resolution;
        private String assignedTo;
    }

    /**
     * 异常统计响应
     */
    @Data
    public static class ExceptionStatisticsResponse {
        private Long totalCount;
        private Long openCount;
        private List<TypeStatistics> statistics;

        @Data
        public static class TypeStatistics {
            private String exceptionType;
            private String exceptionLevel;
            private Long count;
            private Long openCount;
        }
    }

    /**
     * 异常趋势响应
     */
    @Data
    public static class ExceptionTrendResponse {
        private String date;
        private Long count;
        private Long criticalCount;

        public ExceptionTrendResponse(String date, Long count, Long criticalCount) {
            this.date = date;
            this.count = count;
            this.criticalCount = criticalCount;
        }
    }
}