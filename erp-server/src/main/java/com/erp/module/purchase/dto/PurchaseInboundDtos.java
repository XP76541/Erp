package com.erp.module.purchase.dto;

import com.erp.module.purchase.entity.PurchaseInbound;
import com.erp.module.purchase.entity.PurchaseInboundItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库单请求 DTO;金额服务端计算,禁止信任前端
 */
public class PurchaseInboundDtos {

    /** 新建草稿请求 */
    @Data
    public static class CreateRequest {

        @NotNull(message = "供应商不能为空")
        private Long supplierId;

        @NotNull(message = "入库仓不能为空")
        private Long warehouseId;

        /** 业务日期,空则服务端取今天 */
        private LocalDate bizDate;

        private String remark;

        @Valid
        @NotEmpty(message = "明细不能为空")
        private List<ItemInput> items;
    }

    @Data
    public static class ItemInput {

        @NotNull(message = "商品不能为空")
        private Long productId;

        /** 空 = 用主表仓库 */
        private Long warehouseId;

        @NotNull
        @DecimalMin(value = "0.0001", message = "数量必须大于 0")
        @Digits(integer = 14, fraction = 4)
        private BigDecimal qty;

        @NotNull
        @DecimalMin(value = "0", message = "进价不能为负")
        @Digits(integer = 16, fraction = 2)
        private BigDecimal price;

        private String note;
    }

    /** 详情响应:主表 + 明细 */
    public record DetailResponse(PurchaseInbound doc, List<PurchaseInboundItem> items) {
    }
}
