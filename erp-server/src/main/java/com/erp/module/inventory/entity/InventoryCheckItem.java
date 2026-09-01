package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存盘点明细
 */
@Data
@TableName("inventory_check_item")
public class InventoryCheckItem {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 盘点单ID
     */
    private Long checkId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 系统库存数量
     */
    private BigDecimal systemQty;

    /**
     * 实际盘点数量
     */
    private BigDecimal actualQty;

    /**
     * 差异数量
     */
    private BigDecimal diffQty;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 状态：NORMAL-正常, DIFF-差异, MISSING-缺失, EXCESS-溢余
     */
    private String status;

    /**
     * 备注
     */
    private String note;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}