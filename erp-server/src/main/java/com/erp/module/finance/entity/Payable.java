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
 * 应付账款:由采购入库单审核动作自动生成(红字退货为负);只能随付款核销改变 paid_amount
 */
@Data
@TableName("payable")
public class Payable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long supplierId;

    /** PURCHASE_IN / PURCHASE_RETURN */
    private String docType;

    private Long docId;

    private String docNo;

    private LocalDate bizDate;

    /** 到期日 = 业务日期 + 供应商账期 */
    private LocalDate dueDate;

    private BigDecimal amount;

    /** 已核销金额,付款核销时事务性累加 */
    private BigDecimal paidAmount;

    /** UNSETTLED / PARTIAL / SETTLED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
