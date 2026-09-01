package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.CollectionRecord;
import com.erp.module.finance.entity.Receivable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Mapper
public interface CollectionRecordMapper extends BaseMapper<CollectionRecord> {

    /**
     * 获取指定应收账款的催收记录
     */
    @Select("SELECT * FROM collection_record WHERE receivable_id = #{receivableId} ORDER BY created_at DESC")
    List<CollectionRecord> getByReceivableId(@Param("receivableId") Long receivableId);

    /**
     * 获取客户的所有催收记录
     */
    @Select("SELECT * FROM collection_record WHERE customer_id = #{customerId} ORDER BY created_at DESC")
    List<CollectionRecord> getByCustomerId(@Param("customerId") Long customerId);

    /**
     * 获取指定日期范围内的催收记录
     */
    @Select("SELECT * FROM collection_record WHERE contact_time BETWEEN #{startDate} AND #{endDate} ORDER BY contact_time DESC")
    List<CollectionRecord> getByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 获取催收统计数据
     */
    @Select("SELECT " +
            "c.contact_result, " +
            "c.next_action, " +
            "COUNT(*) as count, " +
            "SUM(r.remaining_amount) as amount " +
            "FROM collection_record c " +
            "LEFT JOIN receivable r ON c.receivable_id = r.id " +
            "WHERE c.contact_time IS NOT NULL " +
            "GROUP BY c.contact_result, c.next_action")
    List<CollectionStatistics> getCollectionStatistics();

    /**
     * 获取催收失败记录
     */
    @Select("SELECT * FROM collection_record WHERE next_action = #{action} AND created_at > #{sinceDate} ORDER BY created_at")
    List<CollectionRecord> getFailedCollectionRecords(@Param("action") String action, @Param("sinceDate") LocalDate sinceDate);

    @Data
    static class CollectionStatistics {
        private String contactResult;
        private String nextAction;
        private Long count;
        private BigDecimal amount;
    }
}