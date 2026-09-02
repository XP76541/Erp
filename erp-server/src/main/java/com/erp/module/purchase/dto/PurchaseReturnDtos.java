package com.erp.module.purchase.dto;

import com.erp.module.purchase.entity.PurchaseReturn;
import com.erp.module.purchase.entity.PurchaseReturnItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseReturnDtos {
    @Data
    public static class CreateRequest {
        @NotNull(message = "供应商不能为空") private Long supplierId;
        @NotNull(message = "仓库不能为空") private Long warehouseId;
        private LocalDate bizDate;
        private String reason;
        @Valid @NotEmpty(message = "退货明细不能为空") private List<ItemInput> items;
    }

    @Data
    public static class ItemInput {
        @NotNull(message = "原入库明细不能为空") private Long inboundItemId;
        @NotNull(message = "退货数量不能为空") @DecimalMin(value = "0.0001", message = "退货数量必须大于0") private BigDecimal qty;
        private String note;
    }

    @Data
    public static class ListResponse {
        private Long id;
        private String docNo;
        private Long supplierId;
        private Long warehouseId;
        private LocalDate bizDate;
        private String status;
        private String reason;
        private BigDecimal totalAmount;
        private Long auditBy;
        private java.time.LocalDateTime auditAt;
        private java.time.LocalDateTime createdAt;
    }

    public record DetailResponse(PurchaseReturn doc, List<PurchaseReturnItem> items) {
    }
}
