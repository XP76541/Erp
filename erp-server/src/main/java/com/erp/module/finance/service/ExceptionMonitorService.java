package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.entity.ReceivableException;
import com.erp.module.finance.mapper.ReceivableExceptionMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 应收账款异常监控服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExceptionMonitorService {

    private final ReceivableExceptionMapper exceptionMapper;
    private final ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    /**
     * 每小时检查一次异常情况
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void monitorReceivables() {
        log.info("开始应收账款异常监控...");

        try {
            // 检查重复单据
            checkDuplicateDocuments();

            // 检查金额不符
            checkAmountMismatch();

            // 检查信用超限
            checkCreditLimitExceeded();

            // 检查数据一致性
            checkDataConsistency();

            // 检查大额逾期
            checkLargeOverdue();

            log.info("应收账款异常监控完成");
        } catch (Exception e) {
            log.error("应收账款异常监控失败", e);
        }
    }

    /**
     * 检查重复单据
     */
    private void checkDuplicateDocuments() {
        List<Receivable> receivables = receivableMapper.selectList(null);

        // 按单号分组
        java.util.Map<String, List<Receivable>> docNoGroups = receivables.stream()
                .collect(java.util.stream.Collectors.groupingBy(Receivable::getDocNo));

        for (java.util.Map.Entry<String, List<Receivable>> entry : docNoGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                // 创建异常记录
                for (Receivable receivable : entry.getValue()) {
                    createException(receivable, ReceivableException.ExceptionType.DUPLICATE_DOC,
                            "HIGH", "发现重复单据，请检查数据完整性",
                            "可能影响财务报表准确性，建议立即处理");
                }
            }
        }
    }

    /**
     * 检查金额不符
     */
    private void checkAmountMismatch() {
        List<Receivable> receivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .ne(Receivable::getStatus, "SETTLED"));

        for (Receivable receivable : receivables) {
            BigDecimal calculatedRemaining = receivable.getAmount().subtract(receivable.getPaidAmount());
            if (calculatedRemaining.compareTo(receivable.getRemainingAmount()) != 0) {
                // 金额不符，创建异常
                createException(receivable, ReceivableException.ExceptionType.AMOUNT_MISMATCH,
                        "MEDIUM", "应收账款金额计算不一致",
                        "系统计算的剩余金额与记录不符，建议核对原始数据");
            }
        }
    }

    /**
     * 检查信用超限
     */
    private void checkCreditLimitExceeded() {
        List<Customer> customers = customerMapper.selectList(null);

        for (Customer customer : customers) {
            if (customer.getCreditLimit() != null) {
                // 获取该客户未结算应收账款总额
                BigDecimal totalReceivable = receivableMapper.selectList(
                        Wrappers.<Receivable>lambdaQuery()
                                .eq(Receivable::getCustomerId, customer.getId())
                                .ne(Receivable::getStatus, "SETTLED"))
                        .stream()
                        .map(Receivable::getRemainingAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (totalReceivable.compareTo(customer.getCreditLimit()) > 0) {
                    // 信用超限，创建异常
                    Receivable sampleReceivable = receivableMapper.selectList(
                            Wrappers.<Receivable>lambdaQuery()
                                    .eq(Receivable::getCustomerId, customer.getId())
                                    .last("LIMIT 1"))
                            .stream()
                            .findFirst()
                            .orElse(null);

                    if (sampleReceivable != null) {
                        ReceivableException exception = new ReceivableException();
                        exception.setReceivableId(sampleReceivable.getId());
                        exception.setReceivableDocNo(sampleReceivable.getDocNo());
                        exception.setCustomerId(customer.getId());
                        exception.setCustomerName(customer.getName());
                        exception.setExceptionType(ReceivableException.ExceptionType.CREDIT_LIMIT_EXCEEDED.name());
                        exception.setExceptionLevel("HIGH");
                        exception.setDescription("客户应收账款总额超过信用限额");
                        exception.setImpact("存在坏账风险，建议立即停止授信");
                        exception.setSuggestedAction("联系客户付款或调整信用额度");
                        exception.setStatus(ReceivableException.Status.OPEN.name());
                        exception.setCreatedBy("SYSTEM");
                        exception.setCreatedByName("系统自动");
                        exception.setCreatedAt(LocalDateTime.now());

                        exceptionMapper.insert(exception);
                    }
                }
            }
        }
    }

    /**
     * 检查数据一致性
     */
    private void checkDataConsistency() {
        // 检查是否有应收账款对应的客户不存在
        List<Receivable> receivables = receivableMapper.selectList(null);

        for (Receivable receivable : receivables) {
            Customer customer = customerMapper.selectById(receivable.getCustomerId());
            if (customer == null) {
                createException(receivable, ReceivableException.ExceptionType.DATA_INCONSISTENCY,
                        "HIGH", "应收账款关联的客户不存在",
                        "请检查客户数据完整性，可能影响对账");
            }
        }
    }

    /**
     * 检查大额逾期
     */
    private void checkLargeOverdue() {
        List<Receivable> overdueReceivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .gt(Receivable::getDaysOverdue, 90)
                        .lt(Receivable::getRemainingAmount, new BigDecimal("100000")));

        for (Receivable receivable : overdueReceivables) {
            createException(receivable, ReceivableException.ExceptionType.OVERDUE_ALERT,
                    "CRITICAL", "大额应收账款长期逾期",
                    "逾期超过90天且金额较大，建议考虑法律手段");
        }
    }

    /**
     * 创建异常记录
     */
    private void createException(Receivable receivable, ReceivableException.ExceptionType type,
                               String level, String description, String impact) {
        // 检查是否已存在相同类型的异常
        List<ReceivableException> existingExceptions = exceptionMapper.selectList(
                Wrappers.<ReceivableException>lambdaQuery()
                        .eq(ReceivableException::getReceivableId, receivable.getId())
                        .eq(ReceivableException::getExceptionType, type.name())
                        .eq(ReceivableException::getStatus, "OPEN"));

        if (existingExceptions.isEmpty()) {
            ReceivableException exception = new ReceivableException();
            exception.setReceivableId(receivable.getId());
            exception.setReceivableDocNo(receivable.getDocNo());
            exception.setCustomerId(receivable.getCustomerId());
            exception.setCustomerName(receivable.getCustomerName());
            exception.setExceptionType(type.name());
            exception.setExceptionLevel(level);
            exception.setDescription(description);
            exception.setImpact(impact);
            exception.setSuggestedAction("请及时处理此异常");
            exception.setStatus(ReceivableException.Status.OPEN.name());
            exception.setCreatedBy("SYSTEM");
            exception.setCreatedByName("系统自动");
            exception.setCreatedAt(LocalDateTime.now());

            exceptionMapper.insert(exception);
        }
    }

    /**
     * 手动创建异常
     */
    @Transactional
    public Long createManualException(ReceivableDtos.ExceptionRequest request, TokenStore.LoginUser user) {
        Receivable receivable = receivableMapper.selectById(request.getReceivableId());
        if (receivable == null) {
            throw new BusinessException("应收账款记录不存在");
        }

        ReceivableException exception = new ReceivableException();
        exception.setReceivableId(receivable.getId());
        exception.setReceivableDocNo(receivable.getDocNo());
        exception.setCustomerId(receivable.getCustomerId());
        exception.setCustomerName(receivable.getCustomerName());
        exception.setExceptionType(request.getExceptionType());
        exception.setExceptionLevel(request.getExceptionLevel());
        exception.setDescription(request.getDescription());
        exception.setImpact(request.getImpact());
        exception.setSuggestedAction(request.getSuggestedAction());
        exception.setStatus(ReceivableException.Status.OPEN.name());
        exception.setCreatedBy(user.userId().toString());
        exception.setCreatedByName(user.realName());
        exception.setCreatedAt(LocalDateTime.now());

        exceptionMapper.insert(exception);

        // 记录操作日志
        operationLogService.record(user, "exception", "CREATE",
                "RECEIVABLE", receivable.getId(), receivable.getDocNo(),
                "{\"type\":\"" + request.getExceptionType() + "\"}",
                request.getDescription());

        return exception.getId();
    }

    /**
     * 解决异常
     */
    @Transactional
    public void resolveException(Long id, ReceivableDtos.ExceptionResolutionRequest request, TokenStore.LoginUser user) {
        ReceivableException exception = exceptionMapper.selectById(id);
        if (exception == null) {
            throw new BusinessException("异常记录不存在");
        }

        exceptionMapper.updateStatus(id, "RESOLVED", user.userId().toString(),
                LocalDateTime.now(), request.getResolution());

        // 记录操作日志
        operationLogService.record(user, "exception", "RESOLVE",
                "RECEIVABLE", exception.getReceivableId(), exception.getReceivableDocNo(),
                "{\"resolution\":\"" + request.getResolution() + "\"}",
                "解决异常：" + exception.getExceptionType());
    }

    /**
     * 忽略异常
     */
    @Transactional
    public void ignoreException(Long id, String reason, TokenStore.LoginUser user) {
        ReceivableException exception = exceptionMapper.selectById(id);
        if (exception == null) {
            throw new BusinessException("异常记录不存在");
        }

        exceptionMapper.updateStatus(id, "IGNORED", user.userId().toString(),
                LocalDateTime.now(), reason);

        // 记录操作日志
        operationLogService.record(user, "exception", "IGNORE",
                "RECEIVABLE", exception.getReceivableId(), exception.getReceivableDocNo(),
                "{}", "忽略异常：" + reason);
    }

    /**
     * 获取异常统计
     */
    public ReceivableDtos.ExceptionStatisticsResponse getExceptionStatistics() {
        List<ReceivableExceptionMapper.ExceptionStatistics> statistics = exceptionMapper.getExceptionStatistics();

        ReceivableDtos.ExceptionStatisticsResponse response = new ReceivableDtos.ExceptionStatisticsResponse();
        response.setTotalCount(statistics.stream().mapToLong(ReceivableExceptionMapper.ExceptionStatistics::getCount).sum());
        response.setOpenCount(statistics.stream().mapToLong(ReceivableExceptionMapper.ExceptionStatistics::getOpenCount).sum());
        response.setStatistics(statistics.stream()
                .map(stat -> new ReceivableDtos.ExceptionStatisticsResponse.TypeStatistics(
                        stat.getExceptionType(),
                        stat.getExceptionLevel(),
                        stat.getCount(),
                        stat.getOpenCount()))
                .collect(java.util.stream.Collectors.toList()));

        return response;
    }

    /**
     * 获取异常趋势
     */
    public List<ReceivableDtos.ExceptionTrendResponse> getExceptionTrend(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ReceivableExceptionMapper.ExceptionTrend> trends = exceptionMapper.getExceptionTrend(startDate);

        return trends.stream()
                .map(trend -> new ReceivableDtos.ExceptionTrendResponse(
                        trend.getDate(),
                        trend.getCount(),
                        trend.getCriticalCount()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取所有开放异常
     */
    public List<com.erp.module.finance.entity.ReceivableException> getOpenExceptions() {
        return exceptionMapper.selectList(
                Wrappers.<com.erp.module.finance.entity.ReceivableException>lambdaQuery()
                        .eq(com.erp.module.finance.entity.ReceivableException::getStatus, "OPEN"));
    }

    /**
     * 获取高优先级异常
     */
    public List<com.erp.module.finance.entity.ReceivableException> getHighPriorityExceptions() {
        return exceptionMapper.selectList(
                Wrappers.<com.erp.module.finance.entity.ReceivableException>lambdaQuery()
                        .in(com.erp.module.finance.entity.ReceivableException::getExceptionLevel, "HIGH", "CRITICAL")
                        .eq(com.erp.module.finance.entity.ReceivableException::getStatus, "OPEN"));
    }

    /**
     * 获取指定客户的异常
     */
    public List<com.erp.module.finance.entity.ReceivableException> getExceptionsByCustomer(Long customerId) {
        return exceptionMapper.selectList(
                Wrappers.<com.erp.module.finance.entity.ReceivableException>lambdaQuery()
                        .eq(com.erp.module.finance.entity.ReceivableException::getCustomerId, customerId));
    }

    /**
     * 获取异常详情
     */
    public com.erp.module.finance.entity.ReceivableException getExceptionById(Long id) {
        return exceptionMapper.selectById(id);
    }
}