package com.erp.module.inventory.dto;

import com.erp.common.PageResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存调拨相关的DTO
 */
public class InventoryTransferDtos {

    /**
     * 创建调拨单请求
     */
    @Data
    public static class CreateRequest {
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private LocalDate bizDate;
        private String remark;
        private List<ItemInput> items;
    }

    /**
     * 调拨明细输入
     */
    @Data
    public static class ItemInput {
        private Long productId;
        private BigDecimal qty;
        private BigDecimal price;
        private String note;
    }

    /**
     * 调拨单详情响应
     */
    @Data
    public static class DetailResponse {
        private Long id;
        private String docNo;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private LocalDate bizDate;
        private String status;
        private BigDecimal totalAmount;
        private String fromWarehouseName;
        private String toWarehouseName;
        private List<ItemDetail> items;
        private String operatorName;
        private LocalDateTime auditAt;
        private LocalDateTime approvedAt;
        private String remark;

        public DetailResponse(InventoryTransfer entity, List<ItemDetail> items) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.fromWarehouseId = entity.getFromWarehouseId();
            this.toWarehouseId = entity.getToWarehouseId();
            this.bizDate = entity.getBizDate();
            this.status = entity.getStatus();
            this.totalAmount = entity.getTotalAmount();
            this.items = items;
            this.remark = entity.getRemark();
            this.auditAt = entity.getAuditAt();
            this.approvedAt = entity.getApprovedAt();
        }
    }

    /**
     * 调拨明细详情
     */
    @Data
    public static class ItemDetail {
        private Long id;
        private Long productId;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private BigDecimal qty;
        private BigDecimal price;
        private BigDecimal amount;
        private String productName;
        private String fromWarehouseName;
        private String toWarehouseName;
        private String note;

        public ItemDetail(InventoryTransferItem item) {
            this.id = item.getId();
            this.productId = item.getProductId();
            this.fromWarehouseId = item.getFromWarehouseId();
            this.toWarehouseId = item.getToWarehouseId();
            this.qty = item.getQty();
            this.price = item.getPrice();
            this.amount = item.getAmount();
            this.note = item.getNote();
        }
    }

    /**
     * 分页列表响应
     */
    @Data
    public static class ListResponse {
        private Long id;
        private String docNo;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private LocalDate bizDate;
        private String status;
        private BigDecimal totalAmount;
        private String fromWarehouseName;
        private String toWarehouseName;
        private String operatorName;
        private LocalDateTime auditAt;
        private LocalDateTime approvedAt;
        private LocalDateTime createdAt;

        public ListResponse(InventoryTransfer entity) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.fromWarehouseId = entity.getFromWarehouseId();
            this.toWarehouseId = entity.getToWarehouseId();
            this.bizDate = entity.getBizDate();
            this.status = entity.getStatus();
            this.totalAmount = entity.getTotalAmount();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 审核请求
     */
    @Data
    public static class AuditRequest {
        private String ip;
    }

    /**
     * 完成调拨请求
     */
    @Data
    public static class CompleteRequest {
        private String ip;
    }

    /**
     * 取消调拨请求
     */
    @Data
    public static class CancelRequest {
        private String ip;
    }

    /**
     * 按仓库查询响应
     */
    @Data
    public static class WarehouseResponse {
        private Long id;
        private String docNo;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private LocalDate bizDate;
        private String status;
        private BigDecimal totalAmount;
        private String fromWarehouseName;
        private String toWarehouseName;
        private LocalDateTime createdAt;

        public WarehouseResponse(InventoryTransfer entity) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.fromWarehouseId = entity.getFromWarehouseId();
            this.toWarehouseId = entity.getToWarehouseId();
            this.bizDate = entity.getBizDate();
            this.status = entity.getStatus();
            this.totalAmount = entity.getTotalAmount();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 统计响应
     */
    @Data
    public static class StatsResponse {
        private Integer draftCount;
        private Integer auditCount;
        private Integer completedCount;
        private BigDecimal totalAmount;

        public StatsResponse(Integer draftCount, Integer auditCount, Integer completedCount, BigDecimal totalAmount) {
            this.draftCount = draftCount;
            this.auditCount = auditCount;
            this.completedCount = completedCount;
            this.totalAmount = totalAmount;
        }
    }
}