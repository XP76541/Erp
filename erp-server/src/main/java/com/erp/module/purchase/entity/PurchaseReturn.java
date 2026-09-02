package com.erp.module.purchase.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_return")
public class PurchaseReturn {
    @TableId(type = IdType.AUTO) private Long id;
    private String docNo;
    private Long supplierId;
    private Long warehouseId;
    private LocalDate bizDate;
    private String status;
    private String reason;
    private Long auditBy;
    private LocalDateTime auditAt;
    private Long createdBy;
    @TableField(exist = false) private java.math.BigDecimal totalAmount;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
