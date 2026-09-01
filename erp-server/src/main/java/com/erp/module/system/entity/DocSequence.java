package com.erp.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 单据编号序列,行见 doc_sequence 表
 */
@Data
@TableName("doc_sequence")
public class DocSequence {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 编号类型:SO / OUT / PIN / RCV / PAY / SKU... */
    private String docType;

    /** 期间,如 20260831;不按日期的用 ALL */
    private String period;

    /** 下一个序号 */
    private Integer nextNo;
}
