package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 催收记录
 */
@Data
@TableName("collection_record")
public class CollectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receivableId;
    private String receivableDocNo;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private String contactMethod;
    private String contactPerson;
    private String contactTime;
    private String contactResult;
    private String nextAction;
    private String operator;
    private String operatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 联系方式
     */
    public enum ContactMethod {
        PHONE("电话"),
        EMAIL("邮件"),
        VISIT("上门"),
        MESSAGE("短信"),
        WECHAT("微信");

        private final String description;

        ContactMethod(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 联系结果
     */
    public enum ContactResult {
        PROMISED_PAY("承诺付款"),
        PARTIAL_PAY("部分付款"),
        NO_RESPONSE("无回应"),
        DISPUTE("有争议"),
        DIFFICULT("催收困难"),
        SETTLED("已结清"),
        NEED_MORE_INFO("需要更多信息");

        private final String description;

        ContactResult(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 下一步行动
     */
    public enum NextAction {
        FOLLOW_UP("跟进"),
        LEGAL_ACTION("法律行动"),
        WRITE_OFF("核销"),
        TRANSFER("转交第三方"),
        CLOSE("关闭");

        private final String description;

        NextAction(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}