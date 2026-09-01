package com.erp.module.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReceivableListRequest {
    private Long customerId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}