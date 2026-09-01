package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存调拨单
 */
@Data
@TableName("inventory_transfer")
public class InventoryTransfer {

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
     * 调出仓库ID
     */
    private Long fromWarehouseId;

    /**
     * 调入仓库ID
     */
    private Long toWarehouseId;

    /**
     * 业务日期
     */
    private LocalDate bizDate;

    /**
     * 状态：DRAFT-草稿, AUDITED-已审核, COMPLETED-已完成, CANCELLED-已取消
     */
    private String status;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

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
     * 批准人ID
     */
    private Long approvedBy;

    /**
     * 批准时间
     */
    private LocalDateTime approvedAt;

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