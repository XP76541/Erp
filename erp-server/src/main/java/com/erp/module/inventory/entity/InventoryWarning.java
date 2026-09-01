package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存预警
 */
@Data
@TableName("inventory_warning")
public class InventoryWarning {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预警类型：STOCK_OUT-库存不足, STOCK_OVER-库存超量, EXPIRING-临期, SPOILED-呆滞
     */
    private String warningType;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 当前库存数量
     */
    private BigDecimal currentQty;

    /**
     * 预警值
     */
    private BigDecimal warningValue;

    /**
     * 是否激活
     */
    private Boolean isActive;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 解决人ID
     */
    private Long resolvedBy;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}