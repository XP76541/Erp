package com.erp.module.inventory.dto;

import com.erp.common.PageResult;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存盘点相关的DTO
 */
public class InventoryCheckDtos {

    /**
     * 创建盘点单请求
     */
    @Data
    public static class CreateRequest {
        private Long warehouseId;
        private LocalDate checkDate;
        private String checkType; // FULL-全盘, PARTIAL-部分盘
        private String remark;
        private List<ItemInput> items;
    }

    /**
     * 盘点明细输入
     */
    @Data
    public static class ItemInput {
        private Long productId;
        private BigDecimal price;
        private String note;
    }

    /**
     * 提交盘点结果请求
     */
    @Data
    public static class SubmitResultRequest {
        private List<ItemResult> items;
    }

    /**
     * 盘点结果明细
     */
    @Data
    public static class ItemResult {
        private Long productId;
        private BigDecimal actualQty;
        private String note;
    }

    /**
     * 盘点单详情响应
     */
    @Data
    public static class DetailResponse {
        private Long id;
        private String docNo;
        private Long warehouseId;
        private LocalDate checkDate;
        private String status;
        private String checkType;
        private Integer totalItems;
        private BigDecimal totalAmount;
        private Integer diffItems;
        private BigDecimal diffAmount;
        private String warehouseName;
        private String operatorName;
        private LocalDateTime auditAt;
        private LocalDateTime createdAt;
        private String remark;
        private List<ItemDetail> items;

        public DetailResponse(InventoryCheck entity, List<ItemDetail> items) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.warehouseId = entity.getWarehouseId();
            this.checkDate = entity.getCheckDate();
            this.status = entity.getStatus();
            this.checkType = entity.getCheckType();
            this.totalItems = entity.getTotalItems();
            this.totalAmount = entity.getTotalAmount();
            this.diffItems = entity.getDiffItems();
            this.diffAmount = entity.getDiffAmount();
            this.warehouseName = entity.getWarehouseName();
            this.operatorName = entity.getOperatorName();
            this.createdAt = entity.getCreatedAt();
            this.remark = entity.getRemark();
            this.items = items;
        }
    }

    /**
     * 盘点明细详情
     */
    @Data
    public static class ItemDetail {
        private Long id;
        private Long productId;
        private Long warehouseId;
        private BigDecimal systemQty;
        private BigDecimal actualQty;
        private BigDecimal diffQty;
        private BigDecimal price;
        private BigDecimal amount;
        private String status;
        private String productName;
        private String note;

        public ItemDetail(InventoryCheckItem item) {
            this.id = item.getId();
            this.productId = item.getProductId();
            this.warehouseId = item.getWarehouseId();
            this.systemQty = item.getSystemQty();
            this.actualQty = item.getActualQty();
            this.diffQty = item.getDiffQty();
            this.price = item.getPrice();
            this.amount = item.getAmount();
            this.status = item.getStatus();
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
        private Long warehouseId;
        private LocalDate checkDate;
        private String status;
        private String checkType;
        private Integer totalItems;
        private BigDecimal totalAmount;
        private Integer diffItems;
        private BigDecimal diffAmount;
        private String warehouseName;
        private LocalDateTime createdAt;

        public ListResponse(InventoryCheck entity) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.warehouseId = entity.getWarehouseId();
            this.checkDate = entity.getCheckDate();
            this.status = entity.getStatus();
            this.checkType = entity.getCheckType();
            this.totalItems = entity.getTotalItems();
            this.totalAmount = entity.getTotalAmount();
            this.diffItems = entity.getDiffItems();
            this.diffAmount = entity.getDiffAmount();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 开始盘点请求
     */
    @Data
    public static class StartCheckRequest {
        private String ip;
    }

    /**
     * 审核请求
     */
    @Data
    public static class AuditRequest {
        private String ip;
    }

    /**
     * 取消请求
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
        private Long warehouseId;
        private LocalDate checkDate;
        private String status;
        private String checkType;
        private Integer totalItems;
        private BigDecimal totalAmount;
        private Integer diffItems;
        private BigDecimal diffAmount;
        private String warehouseName;
        private LocalDateTime createdAt;

        public WarehouseResponse(InventoryCheck entity) {
            this.id = entity.getId();
            this.docNo = entity.getDocNo();
            this.warehouseId = entity.getWarehouseId();
            this.checkDate = entity.getCheckDate();
            this.status = entity.getStatus();
            this.checkType = entity.getCheckType();
            this.totalItems = entity.getTotalItems();
            this.totalAmount = entity.getTotalAmount();
            this.diffItems = entity.getDiffItems();
            this.diffAmount = entity.getDiffAmount();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 统计响应
     */
    @Data
    public static class StatsResponse {
        private Integer draftCount;
        private Integer checkingCount;
        private Integer auditedCount;
        private BigDecimal totalAmount;
        private BigDecimal diffAmount;

        public StatsResponse(Integer draftCount, Integer checkingCount, Integer auditedCount,
                           BigDecimal totalAmount, BigDecimal diffAmount) {
            this.draftCount = draftCount;
            this.checkingCount = checkingCount;
            this.auditedCount = auditedCount;
            this.totalAmount = totalAmount;
            this.diffAmount = diffAmount;
        }
    }
}