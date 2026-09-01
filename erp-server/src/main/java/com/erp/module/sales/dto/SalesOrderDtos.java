package com.erp.module.sales.dto;

import com.erp.module.sales.entity.SalesOrder;
import com.erp.module.sales.entity.SalesOrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单请求 DTO
 */
public class SalesOrderDtos {

    /** 新建草稿请求 */
    @Data
    public static class CreateRequest {

        @NotNull(message = "客户不能为空")
        private Long customerId;

        @NotNull(message = "业务日期不能为空")
        private LocalDate bizDate;

        @NotNull(message = "销售人员不能为空")
        private Long salespersonId;

        private String remark;

        @Valid
        @NotEmpty(message = "明细不能为空")
        private List<ItemInput> items;
    }

    @Data
    public static class ItemInput {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @NotNull
        @DecimalMin(value = "0.0001", message = "数量必须大于 0")
        @Digits(integer = 14, fraction = 4)
        private BigDecimal qty;

        @NotNull
        @DecimalMin(value = "0", message = "售价不能为负")
        @Digits(integer = 16, fraction = 2)
        private BigDecimal price;

        private String note;
    }

    /** 审核/驳回请求 */
    @Data
    public static class AuditRequest {

        @NotEmpty(message = "操作类型不能为空")
        private String action; // "audit" or "reject"

        private String remark;
    }

    /** 创建出库单请求 */
    @Data
    public static class CreateOutboundRequest {

        @NotNull(message = "出库仓库不能为空")
        private Long warehouseId;

        @NotNull(message = "发货明细不能为空")
        private List<OutboundItemInput> items;

        private String remark;
    }

    @Data
    public static class OutboundItemInput {

        @NotNull(message = "订单明细ID不能为空")
        private Long orderItemId;

        @NotNull
        @DecimalMin(value = "0.0001", message = "发货数量必须大于 0")
        @Digits(integer = 14, fraction = 4)
        private BigDecimal qty;

        private String remark;
    }

    /** 详情响应:主表 + 明细 */
    public record DetailResponse(SalesOrder doc, List<SalesOrderItem> items) {
    }

    /** 列表响应项 */
    @Data
    public static class ListResponse {

        private Long id;
        private String docNo;
        private Long customerId;
        private String customerName;
        private Long salespersonId;
        private String salespersonName;
        private String status;
        private String shipStatus;
        private BigDecimal totalAmount;
        private LocalDate bizDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}