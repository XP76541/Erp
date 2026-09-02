package com.erp.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("purchase_return_item")
public class PurchaseReturnItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long returnId;
    private Integer lineNo;
    private Long inboundItemId;
    private Long productId;
    private Long warehouseId;
    private BigDecimal qty;
    private BigDecimal unitCost;
    private BigDecimal amount;
    private String note;
}
