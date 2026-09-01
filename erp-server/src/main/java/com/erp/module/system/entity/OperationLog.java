package com.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志(只增不改);created_at 由数据库默认值生成
 */
@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 操作人姓名快照 */
    private String userName;

    /** 模块,如 purchase_inbound */
    private String module;

    /** 动作:AUDIT / VOID / PRICE_CHANGE ... */
    private String action;

    /** 目标单据类型 */
    private String docType;

    private Long docId;

    private String docNo;

    /** 变更前后关键值(JSON 文本) */
    private String detail;

    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
