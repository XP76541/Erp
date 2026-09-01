package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存预警配置
 */
@Data
@TableName("inventory_warning_config")
public class InventoryWarningConfig {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 库存下限
     */
    private BigDecimal stockOutLimit;

    /**
     * 库存上限
     */
    private BigDecimal stockOverLimit;

    /**
     * 预警级别：LOW-低, MEDIUM-中, HIGH-高
     */
    private String warningLevel;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}