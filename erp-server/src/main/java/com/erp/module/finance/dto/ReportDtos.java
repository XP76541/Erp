package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报表相关的DTO集合
 */
public class ReportDtos {

    /**
     * 销售日报表请求
     */
    @Data
    public static class SalesDailyReportRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private Long customerId;
        private Long salespersonId;
    }

    /**
     * 销售日报表明细项
     */
    @Data
    public static class SalesDailyReportItem {
        private String docNo;
        private LocalDate businessDate;
        private String customerName;
        private String salespersonName;
        private BigDecimal amount;
        private BigDecimal shippedAmount;
        private String status;
    }

    /**
     * 销售日报表响应
     */
    @Data
    public static class SalesDailyReportResponse {
        private LocalDate reportDate;
        private Long totalOrders;
        private BigDecimal totalAmount;
        private BigDecimal shippedAmount;
        private List<SalesDailyReportItem> orders;
    }

    /**
     * 进销存汇总表请求
     */
    @Data
    public static class InventorySummaryRequest {
        private LocalDate date;
        private Long warehouseId;
        private Long productId;
        /** 仅财务/管理角色可返回成本及库存金额。 */
        private boolean includeCost;
    }

    /**
     * 进销存汇总表明细项
     */
    @Data
    public static class InventorySummaryItem {
        private Long productId;
        private String productName;
        private String productSpec;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal totalValue;
    }

    /**
     * 进销存汇总表响应
     */
    @Data
    public static class InventorySummaryResponse {
        private LocalDate reportDate;
        private Long totalProducts;
        private BigDecimal totalValue;
        private List<InventorySummaryItem> products;
    }

    /**
     * 财务汇总表请求
     */
    @Data
    public static class FinanceSummaryRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        /** SALES 角色只能查看本人销售范围；其他角色可不填。 */
        private Long salespersonId;
    }

    /**
     * 财务汇总表响应
     */
    @Data
    public static class FinanceSummaryResponse {
        private LocalDate reportDate;
        private BigDecimal totalSales;
        private BigDecimal totalPurchases;
        private BigDecimal totalReceivables;
        private BigDecimal totalPayables;
        private BigDecimal totalInventory;
        /** 当前数据模型没有出库成本快照，净利润不作销售额减采购额的伪毛利。 */
        private BigDecimal netProfit;
        private boolean costDataAvailable;
    }
}