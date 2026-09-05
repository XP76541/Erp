package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.StatementAdjustment;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.StatementAdjustmentMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.finance.mapper.ReceiptMapper;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.OperationLogService;
import com.erp.module.system.service.SystemAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 对账单调整服务
 */
@Service
public class StatementAdjustmentService {

    private final StatementAdjustmentMapper adjustmentMapper;
    private final ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final ReceiptMapper receiptMapper;
    private final OperationLogService operationLogService;
    private final SystemAuthorizationService authorizationService;

    public StatementAdjustmentService(StatementAdjustmentMapper adjustmentMapper,
                                    ReceivableMapper receivableMapper,
                                    CustomerMapper customerMapper,
                                    ReceiptMapper receiptMapper,
                                    OperationLogService operationLogService,
                                    SystemAuthorizationService authorizationService) {
        this.adjustmentMapper = adjustmentMapper;
        this.receivableMapper = receivableMapper;
        this.customerMapper = customerMapper;
        this.receiptMapper = receiptMapper;
        this.operationLogService = operationLogService;
        this.authorizationService = authorizationService;
    }

    /**
     * 创建对账单调整
     */
    @Transactional
    public Long createAdjustment(ReceivableDtos.StatementAdjustmentRequest request, TokenStore.LoginUser user) {
        if (request == null || request.getCustomerId() == null || request.getAdjustmentDate() == null
                || request.getAdjustmentAmount() == null || request.getAdjustmentType() == null
                || request.getAdjustmentAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("调整请求参数无效");
        }
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        // 验证调整金额
        StatementAdjustment adjustment = new StatementAdjustment();
        adjustment.setStatementId(request.getStatementId());
        adjustment.setCustomerId(request.getCustomerId());
        adjustment.setCustomerName(customer.getName());
        adjustment.setAdjustmentDate(request.getAdjustmentDate());
        adjustment.setAdjustmentAmount(request.getAdjustmentAmount());
        adjustment.setAdjustmentType(request.getAdjustmentType());
        adjustment.setReason(request.getReason());
        adjustment.setRemark(request.getRemark());
        adjustment.setOperator(String.valueOf(user.userId()));
        adjustment.setOperatorName(user.realName());
        adjustment.setCreatedAt(java.time.LocalDateTime.now());

        adjustmentMapper.insert(adjustment);

        // 记录操作日志
        operationLogService.record(user, "statement", "ADJUSTMENT",
                "STATEMENT", adjustment.getId(), null,
                "{\"amount\":" + request.getAdjustmentAmount() +
                 ",\"type\":\"" + request.getAdjustmentType() + "\"}",
                request.getReason());

        return adjustment.getId();
    }

    /**
     * 获取指定客户的调整记录
     */
    public List<ReceivableDtos.StatementAdjustmentResponse> getAdjustmentsByCustomer(Long customerId,
                                                                                   LocalDate startDate,
                                                                                   LocalDate endDate,
                                                                                   TokenStore.LoginUser user) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        if (startDate == null) startDate = LocalDate.of(1970, 1, 1);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) throw new BusinessException("日期范围无效");
        List<StatementAdjustment> adjustments = adjustmentMapper.getByCustomerAndDateRange(customerId, startDate, endDate);

        return adjustments.stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定对账单的调整记录
     */
    public List<ReceivableDtos.StatementAdjustmentResponse> getAdjustmentsByStatement(Long statementId,
                                                                                     TokenStore.LoginUser user) {
        List<StatementAdjustment> adjustments = adjustmentMapper.getByStatementId(statementId);
        if (!adjustments.isEmpty()) {
            Customer customer = customerMapper.selectById(adjustments.get(0).getCustomerId());
            if (customer == null) throw new BusinessException("客户不存在");
            authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        }
        return adjustments.stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /** 兼容内部调用，控制器等外部入口必须传入当前用户。 */
    public List<ReceivableDtos.StatementAdjustmentResponse> getAdjustmentsByStatement(Long statementId) {
        return getAdjustmentsByStatement(statementId, TokenStore.getCurrentLoginUser());
    }

    /**
     * 更新调整记录
     */
    @Transactional
    public void updateAdjustment(Long id, ReceivableDtos.StatementAdjustmentRequest request, TokenStore.LoginUser user) {
        StatementAdjustment adjustment = adjustmentMapper.selectById(id);
        if (adjustment == null) {
            throw new BusinessException("调整记录不存在");
        }
        Customer customer = customerMapper.selectById(adjustment.getCustomerId());
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        if (request == null || request.getAdjustmentDate() == null || request.getAdjustmentAmount() == null
                || request.getAdjustmentType() == null || request.getAdjustmentAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("调整请求参数无效");
        }

        adjustment.setAdjustmentAmount(request.getAdjustmentAmount());
        adjustment.setAdjustmentType(request.getAdjustmentType());
        adjustment.setReason(request.getReason());
        adjustment.setRemark(request.getRemark());
        adjustment.setUpdatedAt(java.time.LocalDateTime.now());

        adjustmentMapper.updateById(adjustment);

        // 记录操作日志
        operationLogService.record(user, "statement", "UPDATE_ADJUSTMENT",
                "STATEMENT", adjustment.getId(), null,
                "{\"amount\":" + request.getAdjustmentAmount() +
                 ",\"type\":\"" + request.getAdjustmentType() + "\"}",
                request.getRemark());
    }

    /**
     * 删除调整记录
     */
    @Transactional
    public void deleteAdjustment(Long id, TokenStore.LoginUser user) {
        StatementAdjustment adjustment = adjustmentMapper.selectById(id);
        if (adjustment == null) {
            throw new BusinessException("调整记录不存在");
        }

        Customer customer = customerMapper.selectById(adjustment.getCustomerId());
        if (customer == null) throw new BusinessException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        adjustmentMapper.deleteById(id);

        // 记录操作日志
        operationLogService.record(user, "statement", "DELETE_ADJUSTMENT",
                "STATEMENT", adjustment.getId(), null,
                "{\"amount\":" + adjustment.getAdjustmentAmount() +
                 ",\"type\":\"" + adjustment.getAdjustmentType() + "\"}",
                "删除调整记录");
    }

    /**
     * 获取调整统计
     */
    public List<ReceivableDtos.AdjustmentStatisticsResponse> getAdjustmentStatistics(LocalDate startDate, LocalDate endDate,
                                                                                  TokenStore.LoginUser user) {
        List<StatementAdjustmentMapper.AdjustmentStatistics> statistics;
        Long scope = authorizationService.salespersonScope(user);
        if (scope == null) {
            statistics = adjustmentMapper.getAdjustmentStatistics(startDate, endDate);
        } else {
            List<Long> customerIds = customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                            .eq(Customer::getSalespersonId, scope))
                    .stream().map(Customer::getId).toList();
            if (customerIds.isEmpty()) return List.of();
            statistics = adjustmentMapper.getAdjustmentStatisticsByCustomers(startDate, endDate, customerIds);
        }

        return statistics.stream()
                .map(stat -> new ReceivableDtos.AdjustmentStatisticsResponse(
                        stat.getAdjustmentType(),
                        stat.getCount().intValue(),
                        stat.getTotalAmount()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 生成对账单包含调整
     */
    @Transactional
    public ReceivableDtos.StatementResponse generateStatementWithAdjustments(Long customerId,
                                                                          LocalDate startDate,
                                                                          LocalDate endDate) {
        return generateStatementWithAdjustments(customerId, startDate, endDate, null);
    }

    public ReceivableDtos.StatementResponse generateStatementWithAdjustments(Long customerId,
                                                                          LocalDate startDate,
                                                                          LocalDate endDate,
                                                                          TokenStore.LoginUser user) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new BusinessException("对账单日期范围无效");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        List<Receivable> receivables = receivableMapper.getByCustomerAndDateRange(customerId, startDate, endDate);
        List<Receivable> prevReceivables = receivableMapper.getByCustomerBeforeDate(customerId, startDate);

        // 按收款单业务日期重建期间口径，不能使用当前应收累计 received_amount 代替历史收款。
        BigDecimal priorReceivables = prevReceivables.stream()
                .map(Receivable::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal priorPayments = nullSafe(receiptMapper.sumAllocatedByCustomerBeforeDate(customerId, startDate));
        BigDecimal priorAdjustments = adjustmentMapper.getByCustomerAndDateRange(customerId,
                        LocalDate.of(1970, 1, 1), startDate.minusDays(1)).stream()
                .map(StatementAdjustment::getAdjustmentAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingBalance = priorReceivables.subtract(priorPayments).subtract(priorAdjustments);

        BigDecimal currentReceivables = receivables.stream()
                .map(Receivable::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payments = nullSafe(receiptMapper.sumAllocatedByCustomerAndDateRange(customerId, startDate, endDate));

        // 获取调整金额
        BigDecimal adjustments = adjustmentMapper.getByCustomerAndDateRange(customerId, startDate, endDate)
                .stream()
                .map(StatementAdjustment::getAdjustmentAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closingBalance = openingBalance.add(currentReceivables).subtract(payments).subtract(adjustments);

        // 构建对账单明细
        List<ReceivableDtos.StatementResponse.StatementDetail> details = receivables.stream()
                .map(receivable -> {
                    ReceivableDtos.StatementResponse.StatementDetail detail = new ReceivableDtos.StatementResponse.StatementDetail();
                    detail.setDate(receivable.getBusinessDate());
                    detail.setDocNo(receivable.getDocNo());
                    detail.setDocType("应收账款");
                    detail.setAmount(receivable.getAmount());
                    detail.setPaid(receivable.getPaidAmount());
                    detail.setRemaining(receivable.getRemainingAmount());
                    detail.setStatus(receivable.getStatus());
                    return detail;
                })
                .collect(java.util.stream.Collectors.toList());

        List<StatementAdjustment> adjustmentList = adjustmentMapper.getByCustomerAndDateRange(customerId, startDate, endDate);

        for (StatementAdjustment adjustment : adjustmentList) {
            ReceivableDtos.StatementResponse.StatementDetail detail = new ReceivableDtos.StatementResponse.StatementDetail();
            detail.setDate(adjustment.getAdjustmentDate());
            detail.setDocNo("调整-" + adjustment.getId());
            detail.setDocType("调整");
            detail.setAmount(adjustment.getAdjustmentAmount());
            detail.setPaid(BigDecimal.ZERO);
            detail.setRemaining(BigDecimal.ZERO);
            detail.setStatus("ADJUSTMENT");
            detail.setRemark(adjustment.getReason());
            details.add(detail);
        }

        return new ReceivableDtos.StatementResponse(
                customerId,
                customer.getName(),
                endDate,
                openingBalance,
                currentReceivables,
                payments,
                adjustments,
                closingBalance,
                details);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
    // 私有辅助方法
    private ReceivableDtos.StatementAdjustmentResponse convertToResponse(StatementAdjustment adjustment) {
        ReceivableDtos.StatementAdjustmentResponse response = new ReceivableDtos.StatementAdjustmentResponse();
        response.setId(adjustment.getId());
        response.setCustomerId(adjustment.getCustomerId());
        response.setCustomerName(adjustment.getCustomerName());
        response.setAdjustmentDate(adjustment.getAdjustmentDate());
        response.setAdjustmentAmount(adjustment.getAdjustmentAmount());
        response.setAdjustmentType(adjustment.getAdjustmentType());
        response.setReason(adjustment.getReason());
        response.setRemark(adjustment.getRemark());
        response.setOperator(adjustment.getOperatorName());
        response.setCreatedAt(adjustment.getCreatedAt().toString());
        return response;
    }
}