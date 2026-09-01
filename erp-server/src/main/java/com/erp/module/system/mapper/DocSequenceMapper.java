package com.erp.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.module.system.entity.DocSequence;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DocSequenceMapper extends BaseMapper<DocSequence> {

    /** 原子自增,行锁保证并发不重号;返回 0 表示该类型首号未初始化 */
    @Update("UPDATE doc_sequence SET next_no = next_no + 1 WHERE doc_type = #{docType} AND period = #{period}")
    int increment(@Param("docType") String docType, @Param("period") String period);

    @Select("SELECT next_no FROM doc_sequence WHERE doc_type = #{docType} AND period = #{period}")
    Integer selectNext(@Param("docType") String docType, @Param("period") String period);
}
