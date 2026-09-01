package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InventorySummaryResponse {
    private LocalDate reportDate;
    private Long totalProducts;
    private BigDecimal totalValue;
    private List<InventorySummaryItem> products;
}