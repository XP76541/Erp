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

    private String docNo;

    private Long orderId;

    private String orderDocNo;

    private Long customerId;

    private String customerName;

    @TableField("biz_date")
    private LocalDate businessDate;

    private LocalDate dueDate;

    private BigDecimal amount;

    /** 已核销金额,收款核销时事务性累加 */
    @TableField("received_amount")
    private BigDecimal paidAmount;

    @TableField("remaining_amount")
    private BigDecimal remainingAmount;

    /** UNSETTLED / PARTIAL / SETTLED */
    private String status;

    /** 逾期天数 */
    private Integer daysOverdue;

    /** 账龄分类 */
    private String agingBucket;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 统计信息内部类
     */
    @Data
    public static class ReceivableStatistics {
        private Long customerId;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
        private BigDecimal unsettledAmount;
        private BigDecimal partialAmount;
        private BigDecimal settledAmount;
    }

    /**
     * 账龄分析内部类
     */
    @Data
    public static class AgingAnalysis {
        private String agingBucket;
        private BigDecimal totalAmount;
        private BigDecimal totalPaid;
        private BigDecimal totalRemaining;
    }
}