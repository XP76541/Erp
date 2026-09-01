package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryCheck;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存盘点Mapper
 */
public interface InventoryCheckMapper extends BaseMapper<InventoryCheck> {

    /**
     * 状态机抢占：DRAFT→AUDITING 原子迁移,返回 0 表示已盘点或不存在
     */
    @Update("UPDATE inventory_check SET status = 'AUDITING', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimCheck(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 状态机抢占：AUDITING→AUDITED 原子迁移,返回 0 表示已审核或不存在
     */
    @Update("UPDATE inventory_check SET status = 'AUDITED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'AUDITING'")
    int claimAudit(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 状态机抢占：DRAFT→CANCELLED 原子迁移,返回 0 表示已取消或不存在
     */
    @Update("UPDATE inventory_check SET status = 'CANCELLED', audit_by = #{userId}, audit_at = SYSDATETIME() " +
            "WHERE id = #{id} AND status = 'DRAFT'")
    int claimCancel(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据仓库ID和盘点日期查询盘点单
     */
    @Select("SELECT * FROM inventory_check WHERE warehouse_id = #{warehouseId} AND check_date = #{checkDate}")
    List<InventoryCheck> selectByWarehouseAndDate(@Param("warehouseId") Long warehouseId,
                                                  @Param("checkDate") LocalDate checkDate);

    /**
     * 查询指定日期范围内的盘点单
     */
    @Select("SELECT * FROM inventory_check WHERE check_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY check_date DESC, id DESC")
    List<InventoryCheck> selectByDateRange(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * 查询待完成的盘点单
     */
    @Select("SELECT * FROM inventory_check WHERE status IN ('DRAFT', 'AUDITING') " +
            "ORDER BY created_at DESC")
    List<InventoryCheck> selectPendingChecks();

    /**
     * 更新盘点统计数据
     */
    @Update("UPDATE inventory_check SET " +
            "total_items = #{totalItems}, " +
            "total_amount = #{totalAmount}, " +
            "diff_items = #{diffItems}, " +
            "diff_amount = #{diffAmount} " +
            "WHERE id = #{checkId}")
    int updateCheckStats(@Param("checkId") Long checkId,
                        @Param("totalItems") Integer totalItems,
                        @Param("totalAmount") java.math.BigDecimal totalAmount,
                        @Param("diffItems") Integer diffItems,
                        @Param("diffAmount") java.math.BigDecimal diffAmount);
}