package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AgingAnalysisResponse {
    private String agingBucket;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
}