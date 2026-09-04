package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/** 客户收款核销明细。 */
@Data
@TableName("receipt_allocation")
public class ReceiptAllocation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long receiptId;
    private Long receivableId;
    private BigDecimal amount;
}
