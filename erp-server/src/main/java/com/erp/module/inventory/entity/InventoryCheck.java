package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存盘点单
 */
@Data
@TableName("inventory_check")
public class InventoryCheck {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 单据编号
     */
    private String docNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 盘点日期
     */
    private LocalDate checkDate;

    /**
     * 状态：DRAFT-草稿, AUDITING-盘点中, AUDITED-已盘点, CANCELLED-已取消
     */
    private String status;

    /**
     * 盘点类型：FULL-全盘, PARTIAL-部分盘
     */
    private String checkType;

    /**
     * 总项目数
     */
    private Integer totalItems;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 差异数量
     */
    private Integer diffItems;

    /**
     * 差异金额
     */
    private BigDecimal diffAmount;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 审核人ID
     */
    private Long auditBy;

    /**
     * 审核时间
     */
    private LocalDateTime auditAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}