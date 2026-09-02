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
 * 采购入库单主表(F201):审核 = 单一大事务写库存+台账+应付+日志
 */
@Data
@TableName("purchase_inbound")
public class PurchaseInbound {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** PINyyyyMMdd-nnnn,审核前草稿即取号 */
    private String docNo;

    private Long supplierId;

    /** 来源单据类型,如 PURCHASE_ORDER;直接入库为空 */
    private String docType;

    /** 来源单据ID */
    private Long docId;

    /** 默认入库仓,明细行可逐行覆盖 */
    private Long warehouseId;

    /** 业务日期,影响账期与报表 */
    private LocalDate bizDate;

    /** DRAFT / AUDITED / VOID */
    private String status;

    private Long auditBy;

    private LocalDateTime auditAt;

    private String remark;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
