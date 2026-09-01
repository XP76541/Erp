package com.erp.module.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class InventorySummaryRequest {
    private LocalDate date;
    private Long warehouseId;
    private Long productId;
}