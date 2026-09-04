package com.erp.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.inventory.entity.InventoryWarningLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 库存预警日志Mapper
 */
public interface InventoryWarningLogMapper extends BaseMapper<InventoryWarningLog> {

    /**
     * 按预警查询日志，按操作时间倒序
     */
    @Select("SELECT * FROM inventory_warning_log " +
            "WHERE warning_id = #{warningId} ORDER BY operation_time DESC, id DESC")
    List<InventoryWarningLog> selectByWarningId(@Param("warningId") Long warningId);
}
