package com.erp.module.finance;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收账款:由销售出库单审核动作自动生成
 */
@Data
@TableName("receivable")
public class Receivable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String docNo;

    private Long orderId;

    private String orderDocNo;

    private Long customerId;

    private String customerName;

    private LocalDate businessDate;

    private LocalDate dueDate;

    private BigDecimal amount;

    /** 已核销金额,收款核销时事务性累加 */
    private BigDecimal paidAmount;

    private BigDecimal remainingAmount;

    /** UNSETTLED / PARTIAL / SETTLED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}