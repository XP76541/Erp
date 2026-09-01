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

/**
 * 应收账款实体类
 */
@Data
@TableName("receivable")
public class Receivable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long customerId;
    private String docType;
    private Long docId;
    private String docNo;
    private LocalDate bizDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private String status; // UNSETTLED/PART_SETTLED/SETTLED
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}