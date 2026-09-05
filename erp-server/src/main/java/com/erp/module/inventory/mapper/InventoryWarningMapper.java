package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryWarning;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 库存预警Mapper
 */
public interface InventoryWarningMapper extends BaseMapper<InventoryWarning> {

    /**
     * 根据预警类型和仓库ID查询激活的预警
     */
    @Select({"<script>",
            "SELECT * FROM inventory_warning WHERE warning_type = #{warningType} AND is_active = 1 ",
            "<if test='warehouseId != null'>AND warehouse_id = #{warehouseId}</if>",
            "ORDER BY created_at DESC", "</script>"})
    List<InventoryWarning> selectActiveWarnings(@Param("warningType") String warningType,
                                               @Param("warehouseId") Long warehouseId);

    /**
     * 查询指定仓库的所有激活预警
     */
    @Select({"<script>",
            "SELECT * FROM inventory_warning WHERE is_active = 1 ",
            "<if test='warehouseId != null'>AND warehouse_id = #{warehouseId}</if>",
            "ORDER BY created_at DESC", "</script>"})
    List<InventoryWarning> selectAllActiveWarnings(@Param("warehouseId") Long warehouseId);

    /**
     * 根据商品ID和仓库ID查询激活的预警
     */
    @Select("SELECT * FROM inventory_warning " +
            "WHERE warning_type = #{warningType} AND product_id = #{productId} " +
            "AND warehouse_id = #{warehouseId} AND is_active = 1 " +
            "ORDER BY created_at DESC")
    List<InventoryWarning> selectActiveWarningsByProduct(@Param("warningType") String warningType,
                                                        @Param("productId") Long productId,
                                                        @Param("warehouseId") Long warehouseId);

    /**
     * 解决预警（标记为已解决）
     */
    @Update("UPDATE inventory_warning SET is_active = 0, resolved_at = SYSDATETIME(), resolved_by = #{userId} " +
            "WHERE id = #{warningId} AND is_active = 1")
    int resolveWarning(@Param("warningId") Long warningId, @Param("userId") Long userId);

    /**
     * 更新预警的当前库存数量
     */
    @Update("UPDATE inventory_warning SET current_qty = #{currentQty} " +
            "WHERE id = #{warningId}")
    int updateCurrentQty(@Param("warningId") Long warningId, @Param("currentQty") java.math.BigDecimal currentQty);

    /**
     * 查询逾期未解决的预警
     */
    @Select("SELECT * FROM inventory_warning WHERE is_active = 1 AND resolved_at IS NULL " +
            "AND created_at < DATEADD(DAY, -7, SYSDATETIME()) " +
            "ORDER BY created_at ASC")
    List<InventoryWarning> selectOverdueWarnings();

    /**
     * 批量解决预警
     */
    @Update("<script>" +
            "UPDATE inventory_warning SET is_active = 0, resolved_at = SYSDATETIME(), resolved_by = #{userId} " +
            "WHERE id IN " +
            "<foreach collection='warningIds' item='warningId' open='(' separator=',' close=')'>" +
            "#{warningId} " +
            "</foreach>" +
            "AND is_active = 1" +
            "</script>")
    int batchResolveWarnings(@Param("warningIds") List<Long> warningIds, @Param("userId") Long userId);
}