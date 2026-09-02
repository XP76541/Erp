package com.erp.module.inventory.dto;

import com.erp.module.inventory.entity.InventoryWarning;
import com.erp.module.inventory.entity.InventoryWarningConfig;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存预警相关的DTO
 */
public class InventoryWarningDtos {

    /**
     * 预警类型枚举
     */
    public enum WarningType {
        STOCK_OUT("库存不足"),
        STOCK_OVER("库存超量"),
        EXPIRING("临期预警"),
        SPOILED("呆滞预警");

        private final String description;

        WarningType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 预警级别枚举
     */
    public enum WarningLevel {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高");

        private final String description;

        WarningLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 分页列表响应
     */
    @Data
    public static class ListResponse {
        private Long id;
        private String warningType;
        private Long warehouseId;
        private Long productId;
        private BigDecimal currentQty;
        private BigDecimal warningValue;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String productName;
        private String warehouseName;

        public ListResponse(InventoryWarning entity) {
            this.id = entity.getId();
            this.warningType = entity.getWarningType();
            this.warehouseId = entity.getWarehouseId();
            this.productId = entity.getProductId();
            this.currentQty = entity.getCurrentQty();
            this.warningValue = entity.getWarningValue();
            this.isActive = entity.getIsActive();
            this.createdAt = entity.getCreatedAt();
            this.resolvedAt = entity.getResolvedAt();
        }
    }

    /**
     * 预警详情响应
     */
    @Data
    public static class DetailResponse {
        private Long id;
        private String warningType;
        private Long warehouseId;
        private Long productId;
        private BigDecimal currentQty;
        private BigDecimal warningValue;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String resolvedBy;
        private String remark;
        private String productName;
        private String warehouseName;
        private List<WarningLog> logs;

        public DetailResponse(InventoryWarning entity, List<WarningLog> logs) {
            this.id = entity.getId();
            this.warningType = entity.getWarningType();
            this.warehouseId = entity.getWarehouseId();
            this.productId = entity.getProductId();
            this.currentQty = entity.getCurrentQty();
            this.warningValue = entity.getWarningValue();
            this.isActive = entity.getIsActive();
            this.createdAt = entity.getCreatedAt();
            this.resolvedAt = entity.getResolvedAt();
            this.remark = entity.getRemark();
            this.logs = logs;
        }
    }

    /**
     * 预警日志
     */
    @Data
    public static class WarningLog {
        private Long id;
        private Long warningId;
        private BigDecimal oldQty;
        private BigDecimal newQty;
        private Long operatorId;
        private LocalDateTime operationTime;
        private String operationType;
        private String remark;
        private String operatorName;

    }

    /**
     * 按仓库查询响应
     */
    @Data
    public static class WarehouseResponse {
        private Long id;
        private String warningType;
        private Long warehouseId;
        private Long productId;
        private BigDecimal currentQty;
        private BigDecimal warningValue;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private String productName;
        private String warehouseName;

        public WarehouseResponse(InventoryWarning entity) {
            this.id = entity.getId();
            this.warningType = entity.getWarningType();
            this.warehouseId = entity.getWarehouseId();
            this.productId = entity.getProductId();
            this.currentQty = entity.getCurrentQty();
            this.warningValue = entity.getWarningValue();
            this.isActive = entity.getIsActive();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 活跃预警响应
     */
    @Data
    public static class ActiveResponse {
        private Long id;
        private String warningType;
        private Long warehouseId;
        private Long productId;
        private BigDecimal currentQty;
        private BigDecimal warningValue;
        private LocalDateTime createdAt;
        private String productName;
        private String warehouseName;
        private String warningLevel;

        public ActiveResponse(InventoryWarning entity) {
            this.id = entity.getId();
            this.warningType = entity.getWarningType();
            this.warehouseId = entity.getWarehouseId();
            this.productId = entity.getProductId();
            this.currentQty = entity.getCurrentQty();
            this.warningValue = entity.getWarningValue();
            this.createdAt = entity.getCreatedAt();
        }
    }

    /**
     * 统计响应
     */
    @Data
    public static class StatsResponse {
        private Integer stockOutCount;
        private Integer stockOverCount;
        private Integer expiringCount;
        private Integer spoiledCount;
        private BigDecimal totalAmount;

        public StatsResponse(Integer stockOutCount, Integer stockOverCount,
                           Integer expiringCount, Integer spoiledCount, BigDecimal totalAmount) {
            this.stockOutCount = stockOutCount;
            this.stockOverCount = stockOverCount;
            this.expiringCount = expiringCount;
            this.spoiledCount = spoiledCount;
            this.totalAmount = totalAmount;
        }
    }

    /**
     * 逾期预警响应
     */
    @Data
    public static class OverdueResponse {
        private Long id;
        private String warningType;
        private Long warehouseId;
        private Long productId;
        private BigDecimal currentQty;
        private BigDecimal warningValue;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
        private String productName;
        private String warehouseName;
        private Integer overdueDays;

        public OverdueResponse(InventoryWarning entity) {
            this.id = entity.getId();
            this.warningType = entity.getWarningType();
            this.warehouseId = entity.getWarehouseId();
            this.productId = entity.getProductId();
            this.currentQty = entity.getCurrentQty();
            this.warningValue = entity.getWarningValue();
            this.createdAt = entity.getCreatedAt();
            this.resolvedAt = entity.getResolvedAt();
            this.overdueDays = calculateOverdueDays(entity.getCreatedAt(), entity.getResolvedAt());
        }

        private Integer calculateOverdueDays(LocalDateTime createdAt, LocalDateTime resolvedAt) {
            if (resolvedAt != null) {
                return (int) java.time.Duration.between(createdAt, resolvedAt).toDays();
            }
            return (int) java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        }
    }

    /**
     * 预警配置响应
     */
    @Data
    public static class WarningConfigResponse {
        private Long id;
        private Long productId;
        private Long warehouseId;
        private BigDecimal stockOutLimit;
        private BigDecimal stockOverLimit;
        private String warningLevel;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String productName;
        private String warehouseName;

        public WarningConfigResponse(InventoryWarningConfig entity) {
            this.id = entity.getId();
            this.productId = entity.getProductId();
            this.warehouseId = entity.getWarehouseId();
            this.stockOutLimit = entity.getStockOutLimit();
            this.stockOverLimit = entity.getStockOverLimit();
            this.warningLevel = entity.getWarningLevel();
            this.isActive = entity.getIsActive();
            this.createdAt = entity.getCreatedAt();
            this.updatedAt = entity.getUpdatedAt();
        }
    }

    /**
     * 解决预警请求
     */
    @Data
    public static class ResolveRequest {
        private String remark;
    }

    /**
     * 创建预警配置请求
     */
    @Data
    public static class CreateConfigRequest {
        private Long productId;
        private Long warehouseId;
        private BigDecimal stockOutLimit;
        private BigDecimal stockOverLimit;
        private String warningLevel;
        private Boolean isActive;
    }

    /**
     * 更新预警配置请求
     */
    @Data
    public static class UpdateConfigRequest {
        private BigDecimal stockOutLimit;
        private BigDecimal stockOverLimit;
        private String warningLevel;
        private Boolean isActive;
    }
}