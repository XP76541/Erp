package com.erp.module.finance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.finance.entity.Receivable;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应收账款Mapper接口
 */
@Mapper
public interface ReceivableMapper extends BaseMapper<Receivable> {
}