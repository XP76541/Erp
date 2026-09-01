package com.erp.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购入库单明细:amount = qty × price 由服务端计算,禁止信任前端金额
 */
@Data
@TableName("purchase_inbound_item")
public class PurchaseInboundItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long inboundId;

    private Integer lineNo;

    private Long productId;

    /** 覆盖主表默认仓 */
    private Long warehouseId;

    private BigDecimal qty;

    private BigDecimal price;

    private BigDecimal amount;

    private String note;
}
