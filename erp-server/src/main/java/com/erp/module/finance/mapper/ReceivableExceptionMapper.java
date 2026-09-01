package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.ReceivableException;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Mapper
public interface ReceivableExceptionMapper extends BaseMapper<ReceivableException> {

    /**
     * 获取指定客户的异常记录
     */
    @Select("SELECT * FROM receivable_exception WHERE customer_id = #{customerId} ORDER BY created_at DESC")
    List<ReceivableException> getByCustomerId(@Param("customerId") Long customerId);

    /**
     * 获取指定级别的异常记录
     */
    @Select("SELECT * FROM receivable_exception WHERE exception_level = #{level} ORDER BY created_at DESC")
    List<ReceivableException> getByLevel(@Param("level") String level);

    /**
     * 获取待处理的异常记录
     */
    @Select("SELECT * FROM receivable_exception WHERE status = 'OPEN' ORDER BY exception_level DESC, created_at DESC")
    List<ReceivableException> getOpenExceptions();

    /**
     * 获取高优先级异常记录
     */
    @Select("SELECT * FROM receivable_exception WHERE exception_level IN ('HIGH', 'CRITICAL') AND status = 'OPEN' ORDER BY created_at DESC")
    List<ReceivableException> getHighPriorityExceptions();

    /**
     * 更新异常状态
     */
    @Update("UPDATE receivable_exception SET status = #{status}, " +
            "assigned_to = #{assignedTo}, " +
            "resolved_at = #{resolvedAt}, " +
            "resolution = #{resolution}, " +
            "updated_at = SYSDATETIME() " +
            "WHERE id = #{id}")
    int updateStatus(@Param("id") Long id,
                    @Param("status") String status,
                    @Param("assignedTo") String assignedTo,
                    @Param("resolvedAt") LocalDateTime resolvedAt,
                    @Param("resolution") String resolution);

    /**
     * 获取异常统计
     */
    @Select("SELECT " +
            "exception_type, " +
            "exception_level, " +
            "COUNT(*) as count, " +
            "SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as open_count " +
            "FROM receivable_exception " +
            "GROUP BY exception_type, exception_level")
    List<ExceptionStatistics> getExceptionStatistics();

    /**
     * 获取异常趋势
     */
    @Select("SELECT " +
            "DATE(created_at) as date, " +
            "COUNT(*) as count, " +
            "SUM(CASE WHEN exception_level = 'CRITICAL' THEN 1 ELSE 0 END) as critical_count " +
            "FROM receivable_exception " +
            "WHERE created_at >= #{startDate} " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY date")
    List<ExceptionTrend> getExceptionTrend(@Param("startDate") LocalDateTime startDate);

    @Data
    static class ExceptionStatistics {
        private String exceptionType;
        private String exceptionLevel;
        private Long count;
        private Long openCount;
    }

    @Data
    static class ExceptionTrend {
        private String date;
        private Long count;
        private Long criticalCount;
    }
}