package com.erp.module.sales.entity;

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
@TableName("sales_order")
public class SalesOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docNo;          // SOyyyyMMdd-nnnn
    private Long customerId;
    private Long salespersonId;
    private String status;         // DRAFT/AUDITED/VOID
    private String shipStatus;     // UN_SHIPPED/PART_SHIPPED/SHIPPED
    private BigDecimal totalAmount;
    private LocalDate bizDate;
    private Long auditBy;
    private LocalDateTime auditAt;
    private Long rejectBy;
    private LocalDateTime rejectAt;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}