package com.erp.module.finance.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PaymentListRequest {
    private Long customerId;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer page;
    private Integer size;
}