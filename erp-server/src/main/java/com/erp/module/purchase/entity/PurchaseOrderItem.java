package com.erp.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单明细:amount = qty × price 由服务端计算,禁止信任前端金额
 */
@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Integer lineNo;

    private Long productId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private String note;

    private BigDecimal receivedQty = BigDecimal.ZERO;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}