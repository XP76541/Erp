package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收款核销明细:记录收款单与应收账款的核销关系
 */
@Data
@TableName("payment_allocation")
public class PaymentAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paymentId;

    private Long receivableId;

    private BigDecimal allocatedAmount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}