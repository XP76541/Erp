package com.erp.module.finance.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReceivableCreateRequest {
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