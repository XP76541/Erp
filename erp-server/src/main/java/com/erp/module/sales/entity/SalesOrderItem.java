package com.erp.module.sales.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("sales_order_item")
public class SalesOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Integer lineNo;
    private Long productId;
    private BigDecimal qty;
    private BigDecimal shippedQty;
    private BigDecimal price;
    private BigDecimal amount;
    private String note;
}