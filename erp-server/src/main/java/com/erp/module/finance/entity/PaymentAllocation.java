package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 供应商付款与应付账款的核销关系 */
@Data
@TableName("payment_allocation")
public class PaymentAllocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long paymentId;
    private Long payableId;
    private BigDecimal amount;

    /** 兼容旧客户收款代码,不映射到供应商付款表 */
    @TableField(exist = false)
    private Long receivableId;
    @TableField(exist = false)
    private BigDecimal allocatedAmount;
    @TableField(exist = false)
    private LocalDateTime createdAt;
}
