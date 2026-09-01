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

import java.time.LocalDateTime;

/**
 * 供应商档案;编码自动生成,档案只停用不删除
 */
@Data
@TableName("supplier")
public class Supplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 供应商编码,留空自动生成(SUP + 6 位序号),创建后不可修改 */
    private String code;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过 100 字")
    private String name;

    @Size(max = 50, message = "联系人不能超过 50 字")
    private String contact;

    @Size(max = 20, message = "电话不能超过 20 字")
    private String phone;

    /** 账期(天),0 = 现结;应付到期日 = 业务日期 + 账期 */
    @PositiveOrZero(message = "账期不能为负数")
    private Integer paymentTermDays;

    /** 结算方式:现结 / 月结 等 */
    @Size(max = 10, message = "结算方式不能超过 10 字")
    private String settleType;

    @Size(max = 100, message = "开户行不能超过 100 字")
    private String bankName;

    @Size(max = 50, message = "银行账号不能超过 50 字")
    private String bankAccount;

    /** 1 启用 0 停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
