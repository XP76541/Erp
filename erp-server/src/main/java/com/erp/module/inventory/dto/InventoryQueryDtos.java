package com.erp.module.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 即时库存与库存流水查询 DTO */
public class InventoryQueryDtos {
    @Data
    public static class StockResponse {
        private Long productId;
        private String productCode;
        private String productName;
        private String productSpec;
        private Long categoryId;
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal totalValue;
    }

    @Data
    public static class LedgerResponse {
        private Long id;
        private String docType;
        private Long docId;
        private String docNo;
        private Long productId;
        private String productName;
        private Long warehouseId;
        private String warehouseName;
        private Integer direction;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal amount;
        private BigDecimal balanceQuantity;
        private BigDecimal balanceAmount;
        private LocalDate bizDate;
        private LocalDateTime createdAt;
    }
}
