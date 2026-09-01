package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("payment_allocation")
public class PaymentAllocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paymentId;
    private Long payableId;
    private BigDecimal amount;
}