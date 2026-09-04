package com.erp.module.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存预警变更日志
 */
@Data
@TableName("inventory_warning_log")
public class InventoryWarningLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预警ID
     */
    private Long warningId;

    /**
     * 变更前库存数量
     */
    private BigDecimal oldQty;

    /**
     * 变更后库存数量
     */
    private BigDecimal newQty;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 操作类型：STOCK_IN/STOCK_OUT/TRANSFER/RESOLVE
     */
    private String operationType;

    /**
     * 备注
     */
    private String remark;
}
