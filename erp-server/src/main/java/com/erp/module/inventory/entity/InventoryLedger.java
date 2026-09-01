package com.erp.module.inventory.entity;

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
 * 出入库流水(台账,只增不改);balance_* 为变动后结存快照,审计追溯用
 */
@Data
@TableName("inventory_ledger")
public class InventoryLedger {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** PURCHASE_IN / SALES_OUT / TRANSFER / CHECK_ADJ ... */
    private String docType;

    private Long docId;

    private String docNo;

    private Long productId;

    private Long warehouseId;

    /** 1 入库 / -1 出库 */
    private Integer direction;

    /** 变动数量,恒为正数 */
    private BigDecimal qty;

    private BigDecimal unitCost;

    private BigDecimal amount;

    /** 变动后结存数量快照 */
    private BigDecimal balanceQty;

    /** 变动后结存金额快照 */
    private BigDecimal balanceAmount;

    private LocalDate bizDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
