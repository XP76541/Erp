package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Receipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface ReceiptMapper extends BaseMapper<Receipt> {
    @Select("SELECT * FROM receipt WITH (UPDLOCK, ROWLOCK) WHERE id = #{id}")
    Receipt selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM receipt WHERE idempotency_key = #{key}")
    Receipt selectByIdempotencyKey(@Param("key") String key);

    @Update("UPDATE receipt SET status = #{status}, audit_by = #{userId}, audit_at = SYSDATETIME(), updated_by = #{userId}, updated_at = SYSDATETIME() WHERE id = #{id} AND status = 'DRAFT'")
    int claimAudit(@Param("id") Long id, @Param("status") String status, @Param("userId") Long userId);

    @Select("SELECT COALESCE(SUM(ra.amount), 0) FROM receipt_allocation ra " +
            "JOIN receipt r ON r.id = ra.receipt_id " +
            "WHERE r.customer_id = #{customerId} AND r.biz_date BETWEEN #{startDate} AND #{endDate} " +
            "AND r.status = 'AUDITED'")
    BigDecimal sumAllocatedByCustomerAndDateRange(@Param("customerId") Long customerId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(ra.amount), 0) FROM receipt_allocation ra " +
            "JOIN receipt r ON r.id = ra.receipt_id " +
            "WHERE r.customer_id = #{customerId} AND r.biz_date < #{beforeDate} " +
            "AND r.status = 'AUDITED'")
    BigDecimal sumAllocatedByCustomerBeforeDate(@Param("customerId") Long customerId,
                                                 @Param("beforeDate") LocalDate beforeDate);
}
