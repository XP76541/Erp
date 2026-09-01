package com.erp.module.system.service;

import com.erp.module.system.TokenStore;
import com.erp.module.system.entity.OperationLog;
import com.erp.module.system.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志(只增不改):审核、作废、改价等关键动作必须留痕
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    public void record(TokenStore.LoginUser user, String module, String action,
                       String docType, Long docId, String docNo, String detail, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(user.userId());
        log.setUserName(user.realName());
        log.setModule(module);
        log.setAction(action);
        log.setDocType(docType);
        log.setDocId(docId);
        log.setDocNo(docNo);
        log.setDetail(detail);
        log.setIp(ip);
        operationLogMapper.insert(log);
    }
}
