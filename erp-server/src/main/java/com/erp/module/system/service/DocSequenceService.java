package com.erp.module.system.service;

import com.erp.common.BusinessException;
import com.erp.module.system.entity.DocSequence;
import com.erp.module.system.mapper.DocSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单据取号:行锁 + 唯一键兜底,保证并发不重号,号段作废不回收
 * 见 docs/database-design.md §6.4
 */
@Service
@RequiredArgsConstructor
public class DocSequenceService {

    private final DocSequenceMapper docSequenceMapper;

    /**
     * 取下一个序号(1 起)
     *
     * @param docType 编号类型,如 SO、SKU
     * @param period  期间,如 20260831;不按日期的传 ALL
     */
    @Transactional
    public int next(String docType, String period) {
        if (docSequenceMapper.increment(docType, period) == 0) {
            // 首号未初始化,插入起始行;并发下冲突则重试自增
            DocSequence seq = new DocSequence();
            seq.setDocType(docType);
            seq.setPeriod(period);
            seq.setNextNo(2);
            try {
                docSequenceMapper.insert(seq);
                return 1;
            } catch (DuplicateKeyException e) {
                docSequenceMapper.increment(docType, period);
            }
        }
        Integer next = docSequenceMapper.selectNext(docType, period);
        if (next == null) {
            throw new BusinessException("取号失败:" + docType);
        }
        return next;
    }

    /** 生成完整单据号,如 SO20260831-0001 */
    public String nextDocNo(String prefix, String docType, String period) {
        return prefix + period + String.format("-%04d", next(docType, period));
    }
}
