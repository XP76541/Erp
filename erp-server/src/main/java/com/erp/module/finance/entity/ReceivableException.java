package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应收账款异常记录
 */
@Data
@TableName("receivable_exception")
public class ReceivableException {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receivableId;
    private String receivableDocNo;
    private Long customerId;
    private String customerName;

    private String exceptionType;
    private String exceptionLevel;
    private String description;
    private String impact;
    private String suggestedAction;

    private String status; // OPEN / RESOLVED / IGNORED
    private String assignedTo;
    private LocalDateTime resolvedAt;
    private String resolution;

    private String createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 异常类型
     */
    public enum ExceptionType {
        DUPLICATE_DOC("重复单据"),
        AMOUNT_MISMATCH("金额不符"),
        OVERDUE_ALERT("逾期预警"),
        CREDIT_LIMIT_EXCEEDED("信用超限"),
        DISPUTE_ISSUE("争议问题"),
        SYSTEM_ERROR("系统错误"),
        DATA_INCONSISTENCY("数据不一致"),
        PAYMENT_DELAY("付款延迟"),
        CUSTOMER_COMPLAINT("客户投诉");

        private final String description;

        ExceptionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 异常级别
     */
    public enum ExceptionLevel {
        LOW("低"),
        MEDIUM("中"),
        HIGH("高"),
        CRITICAL("严重");

        private final String description;

        ExceptionLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 异常状态
     */
    public enum Status {
        OPEN("待处理"),
        RESOLVED("已解决"),
        IGNORED("已忽略");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}