package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Payable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface PayableMapper extends BaseMapper<Payable> {

    @Select("SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END FROM payable WHERE doc_type = #{docType} AND doc_id = #{docId}")
    int existsByDocTypeAndDocId(@Param("docType") String docType, @Param("docId") Long docId);

    @Select("SELECT * FROM payable WITH (UPDLOCK, ROWLOCK) WHERE id = #{id}")
    Payable selectForUpdate(@Param("id") Long id);

    @Update("UPDATE payable SET paid_amount = paid_amount + #{amount}, status = CASE WHEN paid_amount + #{amount} >= amount THEN 'SETTLED' ELSE 'PARTIAL' END, updated_at = SYSDATETIME() WHERE id = #{id} AND status IN ('UNSETTLED', 'PARTIAL') AND paid_amount + #{amount} <= amount")
    int updatePaidAmount(@Param("id") Long id, @Param("amount") java.math.BigDecimal amount);
}
