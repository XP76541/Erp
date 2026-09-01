package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryTransfer;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存调拨Mapper
 */
public interface InventoryTransferMapper extends BaseMapper<InventoryTransfer> {

    /**
     * 状态机抢占：DRAFT→AUDIT 原子迁移,返回 0 表示已被审或不存在
     */
    @Update("UPDATE inventory_transfer SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 状态机抢占：AUDITED→COMPLETED 原子迁移,返回 0 表示已完成或不存在
     */
    @Update("UPDATE inventory_transfer SET status = 'COMPLETED', approved_by = #{userId}, approved_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'AUDITED'")
    int claimComplete(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 状态机抢占：DRAFT→CANCELLED 原子迁移,返回 0 表示已取消或不存在
     */
    @Update("UPDATE inventory_transfer SET status = 'CANCELLED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimCancel(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据仓库ID查询调拨列表
     */
    @Select("SELECT * FROM inventory_transfer " +
            "WHERE from_warehouse_id = #{warehouseId} OR to_warehouse_id = #{warehouseId} " +
            "ORDER BY biz_date DESC, id DESC")
    List<InventoryTransfer> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 根据日期范围查询调拨列表
     */
    @Select("SELECT * FROM inventory_transfer " +
            "WHERE biz_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY biz_date DESC, id DESC")
    List<InventoryTransfer> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 查询待完成的调拨单
     */
    @Select("SELECT * FROM inventory_transfer WHERE status IN ('DRAFT', 'AUDITED') " +
            "ORDER BY created_at DESC")
    List<InventoryTransfer> selectPendingTransfers();
}