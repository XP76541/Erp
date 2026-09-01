package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账单调整记录
 */
@Data
@TableName("statement_adjustment")
public class StatementAdjustment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long statementId;
    private Long customerId;
    private String customerName;
    private LocalDate adjustmentDate;
    private BigDecimal adjustmentAmount;
    private String adjustmentType;
    private String reason;
    private String remark;
    private String operator;
    private String operatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 调整类型
     */
    public enum AdjustmentType {
        WRITE_OFF("核销"),
        REVERSAL("冲销"),
        INTEREST("利息调整"),
        DISCOUNT("折扣调整"),
        OTHER("其他调整");

        private final String description;

        AdjustmentType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}