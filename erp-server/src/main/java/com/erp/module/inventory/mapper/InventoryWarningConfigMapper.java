package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryWarningConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

/**
 * 库存预警配置Mapper
 */
public interface InventoryWarningConfigMapper extends BaseMapper<InventoryWarningConfig> {

    /**
     * 根据商品ID查询预警配置
     */
    @Select("SELECT * FROM inventory_warning_config WHERE product_id = #{productId} AND is_active = 1")
    List<InventoryWarningConfig> selectByProductId(@Param("productId") Long productId);

    /**
     * 根据仓库ID查询预警配置
     */
    @Select("SELECT * FROM inventory_warning_config WHERE warehouse_id = #{warehouseId} AND is_active = 1")
    List<InventoryWarningConfig> selectByWarehouseId(@Param("warehouseId") Long warehouseId);

    /**
     * 根据商品ID和仓库ID查询预警配置
     */
    @Select("SELECT * FROM inventory_warning_config " +
            "WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} AND is_active = 1")
    List<InventoryWarningConfig> selectByProductAndWarehouse(@Param("productId") Long productId,
                                                           @Param("warehouseId") Long warehouseId);

    /**
     * 批量更新预警配置的激活状态
     */
    @Update("<script>" +
            "UPDATE inventory_warning_config SET is_active = #{isActive}, updated_at = SYSDATETIME() " +
            "WHERE id IN " +
            "<foreach collection='configIds' item='configId' open='(' separator=',' close=')'>" +
            "#{configId} " +
            "</foreach>" +
            "</script>")
    int batchUpdateActiveStatus(@Param("configIds") List<Long> configIds, @Param("isActive") Boolean isActive);
}