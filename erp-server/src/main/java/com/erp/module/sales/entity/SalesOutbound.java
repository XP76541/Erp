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
@TableName("sales_outbound")
public class SalesOutbound {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docNo;          // OUTyyyyMMdd-nnnn
    private Long orderId;
    private Long customerId;
    private Long warehouseId;
    private String status;
    private LocalDate bizDate;
    private Long auditBy;
    private LocalDateTime auditAt;
    private Long rejectBy;
    private LocalDateTime rejectAt;
    private BigDecimal totalAmount;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}