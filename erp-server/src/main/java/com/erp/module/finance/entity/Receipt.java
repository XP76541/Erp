package com.erp.module.finance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 客户收款单。与供应商付款单(payment)分开映射。 */
@Data
@TableName("receipt")
public class Receipt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String docNo;
    /** 客户端幂等键，重试同一收款请求返回原单。 */
    private String idempotencyKey;
    /** 请求内容稳定摘要，用于检测同一幂等键被复用为不同请求。 */
    private String idempotencyFingerprint;
    private Long customerId;
    private LocalDate bizDate;
    private BigDecimal amount;
    private String method;
    private String bankAccount;
    private String status;
    private Long auditBy;
    private LocalDateTime auditAt;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
