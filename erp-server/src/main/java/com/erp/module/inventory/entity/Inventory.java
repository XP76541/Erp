package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 即时库存(结存表):每商品每仓一行;加权平均成本 = total_cost / qty
 * 数量只能由已审核单据通过 InventoryService 改变,禁止直接改本表
 */
@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Long warehouseId;

    private BigDecimal qty;

    private BigDecimal totalCost;

    /** 乐观锁版本号,配合原子增减 */
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
