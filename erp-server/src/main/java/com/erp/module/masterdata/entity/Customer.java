package com.erp.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户档案;salesperson_id 为数据权限锚点(US-601),0 = 未指定
 */
@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户编码,留空自动生成(CUS + 6 位序号),创建后不可修改 */
    private String code;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过 100 字")
    private String name;

    /** 简称,搜索用 */
    @Size(max = 50, message = "简称不能超过 50 字")
    private String shortName;

    @Size(max = 50, message = "联系人不能超过 50 字")
    private String contact;

    @Size(max = 20, message = "电话不能超过 20 字")
    private String phone;

    /** 默认收货地址 */
    @Size(max = 200, message = "地址不能超过 200 字")
    private String address;

    /** 账期(天),0 = 现结;应收到期日 = 业务日期 + 账期 */
    @PositiveOrZero(message = "账期不能为负数")
    private Integer paymentTermDays;

    /** 信用额度,0 = 不限 */
    @PositiveOrZero(message = "信用额度不能为负数")
    private BigDecimal creditLimit;

    /** 归属业务员(数据权限锚点),0 = 未指定 */
    private Long salespersonId;

    /** 1 启用 0 停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
