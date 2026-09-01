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

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docNo;          // PAYyyyyMMdd-nnnn
    private Long supplierId;
    private LocalDate bizDate;
    private BigDecimal amount;
    private String method;        // 转账/现金/支票...
    private String bankAccount;
    private String status;        // DRAFT/AUDITED/VOID
    private Long auditBy;
    private LocalDateTime auditAt;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}