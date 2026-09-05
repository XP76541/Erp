package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.ReceiptAllocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface ReceiptAllocationMapper extends BaseMapper<ReceiptAllocation> {
    @Select("SELECT * FROM receipt_allocation WHERE receipt_id = #{receiptId}")
    java.util.List<ReceiptAllocation> getByReceiptId(@Param("receiptId") Long receiptId);

    @Select("SELECT COALESCE(SUM(amount), 0) FROM receipt_allocation WHERE receipt_id = #{receiptId}")
    BigDecimal getAllocatedAmount(@Param("receiptId") Long receiptId);
}
