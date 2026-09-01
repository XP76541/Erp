package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Payable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PayableMapper extends BaseMapper<Payable> {

    /** 行锁读取应付行(付款核销时使用,本期预留) */
    @Select("SELECT * FROM payable WITH (UPDLOCK, ROWLOCK) WHERE id = #{id}")
    Payable selectForUpdate(@Param("id") Long id);
}
