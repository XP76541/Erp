package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.StatementAdjustment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Mapper
public interface StatementAdjustmentMapper extends BaseMapper<StatementAdjustment> {

    /**
     * 获取指定日期范围内的调整记录
     */
    @Select("SELECT * FROM statement_adjustment WHERE adjustment_date BETWEEN #{startDate} AND #{endDate} ORDER BY adjustment_date DESC")
    List<StatementAdjustment> getByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取指定客户的调整记录
     */
    @Select("SELECT * FROM statement_adjustment WHERE customer_id = #{customerId} ORDER BY adjustment_date DESC")
    List<StatementAdjustment> getByCustomerId(@Param("customerId") Long customerId);

    /**
     * 获取指定对账单的调整记录
     */
    @Select("SELECT * FROM statement_adjustment WHERE statement_id = #{statementId} ORDER BY adjustment_date")
    List<StatementAdjustment> getByStatementId(@Param("statementId") Long statementId);

    /**
     * 根据调整类型获取统计
     */
    @Select("SELECT " +
            "adjustment_type, " +
            "COUNT(*) as count, " +
            "SUM(adjustment_amount) as total_amount " +
            "FROM statement_adjustment " +
            "WHERE adjustment_date BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY adjustment_type")
    List<AdjustmentStatistics> getAdjustmentStatistics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Data
    static class AdjustmentStatistics {
        private String adjustmentType;
        private Long count;
        private BigDecimal totalAmount;
    }
}