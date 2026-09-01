package com.erp.module.masterdata.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类;两级结构(parentId=0 为根),档案只停用不删除
 */
@Data
@TableName("product_category")
public class ProductCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上级分类,0 = 根分类 */
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称不能超过 50 字")
    private String name;

    /** 排序号,小的在前 */
    private Integer sort;

    /** 1 启用 0 停用 */
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
