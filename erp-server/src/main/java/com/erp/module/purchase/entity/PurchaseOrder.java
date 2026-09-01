package com.erp.module.purchase.entity;

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
 * 采购订单(P201):草稿创建 + 审核
 * 审核 = 单一大事务:状态机 → 审计字段 → 操作日志
 * 任一步失败整体回滚
 */
@Data
@TableName("purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** POyyyyMMdd-nnnn,草稿即取号 */
    private String docNo;

    private Long supplierId;

    private Long warehouseId;

    /** 业务日期 */
    private LocalDate bizDate;

    /** DRAFT / AUDITED / VOID */
    private String status;

    private Long createdBy;
    private Long auditBy;
    private LocalDateTime auditAt;
    private Long rejectBy;
    private LocalDateTime rejectAt;

    private BigDecimal totalAmount;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}