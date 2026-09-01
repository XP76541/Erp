package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存调拨明细
 */
@Data
@TableName("inventory_transfer_item")
public class InventoryTransferItem {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 调拨单ID
     */
    private Long transferId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 调出仓库ID
     */
    private Long fromWarehouseId;

    /**
     * 调入仓库ID
     */
    private Long toWarehouseId;

    /**
     * 数量
     */
    private BigDecimal qty;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 金额
     */
    private BigDecimal amount;

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