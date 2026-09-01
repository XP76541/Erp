package com.erp.module.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FinanceSummaryRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}