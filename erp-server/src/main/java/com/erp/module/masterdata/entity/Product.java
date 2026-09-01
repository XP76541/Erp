package com.erp.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品档案;编码自动生成,档案只停用不删除
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;

    private Long categoryId;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String spec;

    @NotBlank(message = "计量单位不能为空")
    private String unit;

    private String barcode;

    private BigDecimal purchasePrice;
    private BigDecimal salePrice;

    /** 最低限价,订单审核强制校验;0 = 不限 */
    private BigDecimal minSalePrice;

    /** 1 启用 0 停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
