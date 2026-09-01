package com.erp.module.sales.dto;

import com.erp.module.sales.entity.SalesOutbound;
import com.erp.module.sales.entity.SalesOutboundItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售出库单请求 DTO
 */
public class SalesOutboundDtos {

    /** 新建草稿请求 */
    @Data
    public static class CreateRequest {

        @NotNull(message = "订单ID不能为空")
        private Long orderId;

        @NotNull(message = "出库仓库不能为空")
        private Long warehouseId;

        private LocalDate bizDate;

        private String remark;

        @Valid
        @NotEmpty(message = "明细不能为空")
        private List<ItemInput> items;
    }

    @Data
    public static class ItemInput {

        @NotNull(message = "订单明细ID不能为空")
        private Long orderItemId;

        @NotNull
        private BigDecimal qty; // 数量由前端控制，不能超过订单未发货数量

        private String remark;
    }

    /** 审核/驳回请求 */
    @Data
    public static class AuditRequest {

        @NotEmpty(message = "操作类型不能为空")
        private String action; // "audit" or "reject"

        private String remark;
    }

    /** 详情响应:主表 + 明细 */
    public record DetailResponse(SalesOutbound doc, List<SalesOutboundItem> items) {
    }

    /** 列表响应项 */
    @Data
    public static class ListResponse {

        private Long id;
        private String docNo;
        private Long orderId;
        private String orderDocNo;
        private Long customerId;
        private String customerName;
        private Long warehouseId;
        private String warehouseName;
        private String status;
        private BigDecimal totalAmount;
        private LocalDate bizDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** 创建出库单的响应（从销售订单生成） */
    @Data
    public static class CreateFromOrderResponse {

        private Long outboundId;
        private String outboundDocNo;
        private List<SalesOutboundItem> items;
    }
}