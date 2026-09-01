package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InventorySummaryItem {
    private Long productId;
    private String productName;
    private String productSpec;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal totalValue;
}