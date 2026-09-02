package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 供应商付款单,审核后核销应付账款 */
@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docNo;
    private Long supplierId;
    private LocalDate bizDate;
    private BigDecimal amount;
    private String method;
    private String bankAccount;
    private String status;
    private Long auditBy;
    private LocalDateTime auditAt;
    private String remark;
    private Long createdBy;
    @TableField(exist = false)
    private Long customerId;
    @TableField(exist = false)
    private String customerName;
    @TableField(exist = false)
    private LocalDate businessDate;
    @TableField(exist = false)
    private BigDecimal allocatedAmount;
    @TableField(exist = false)
    private String paymentMethod;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
