package com.erp.module.purchase.dto;

import com.erp.module.purchase.entity.PurchaseOrder;
import com.erp.module.purchase.entity.PurchaseOrderItem;
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
 * 采购订单请求 DTO;金额服务端计算,禁止信任前端
 */
public class PurchaseOrderDtos {

    /** 新建草稿请求 */
    @Data
    public static class CreateRequest {

        @NotNull(message = "供应商不能为空")
        private Long supplierId;

        @NotNull(message = "采购仓不能为空")
        private Long warehouseId;

        private LocalDate bizDate;

        @NotEmpty(message = "明细不能为空")
        @Valid
        private List<ItemInput> items;

        private String remark;
    }

    /** 明细行输入 */
    @Data
    public static class ItemInput {

        @NotNull(message = "商品不能为空")
        private Long productId;

        @DecimalMin(value = "0.01", message = "数量必须大于0")
        @Digits(integer = 10, fraction = 3, message = "数量最多10位整数3位小数")
        private BigDecimal qty;

        @DecimalMin(value = "0.01", message = "单价必须大于0")
        @Digits(integer = 10, fraction = 4, message = "单价最多10位整数4位小数")
        private BigDecimal price;

        private String note;
    }

    /** 列表响应 */
    @Data
    public static class ListResponse {

        private Long id;
        private String docNo;
        private Long supplierId;
        private String supplierName;
        private Long warehouseId;
        private String warehouseName;
        private LocalDate bizDate;
        private String status;
        private BigDecimal totalAmount;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /** 详情响应:主表 + 明细 */
    public record DetailResponse(PurchaseOrder order, List<PurchaseOrderItem> items) {
    }

    /** 创建从采购订单响应 */
    @Data
    public static class CreateFromOrderResponse {

        private Long orderId;
        private String orderDocNo;
        private Long inboundId;
        private String inboundDocNo;
    }

    /** 入库进度响应 */
    @Data
    public static class ReceivedProgressResponse {

        private Long orderId;
        private BigDecimal totalOrdered;
        private BigDecimal totalReceived;
        private BigDecimal remaining;
        private BigDecimal progressRate;
    }

    /** 更新采购数量请求 */
    @Data
    public static class UpdateQtyRequest {

        @NotEmpty(message = "明细列表不能为空")
        private List<ItemUpdate> items;
    }

    /** 明细更新 */
    @Data
    public static class ItemUpdate {

        @NotNull(message = "明细ID不能为空")
        private Long itemId;

        @DecimalMin(value = "0.01", message = "数量必须大于0")
        @Digits(integer = 10, fraction = 3, message = "数量最多10位整数3位小数")
        private BigDecimal qty;
    }

    /** 审核请求 */
    @Data
    public static class AuditRequest {

        private String remark;
        private String ip;
    }

    /** 驳回请求 */
    @Data
    public static class RejectRequest {

        private String remark;
        private String ip;
    }
}