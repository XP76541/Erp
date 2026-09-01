package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.StatementAdjustment;
import com.erp.module.finance.mapper.StatementAdjustmentMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.OperationLogService;
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
    private final OperationLogService operationLogService;

    public StatementAdjustmentService(StatementAdjustmentMapper adjustmentMapper,
                                    ReceivableMapper receivableMapper,
                                    CustomerMapper customerMapper,
                                    OperationLogService operationLogService) {
        this.adjustmentMapper = adjustmentMapper;
        this.receivableMapper = receivableMapper;
        this.customerMapper = customerMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 创建对账单调整
     */
    @Transactional
    public Long createAdjustment(ReceivableDtos.StatementAdjustmentRequest request, TokenStore.LoginUser user) {
        // 验证客户
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        // 验证调整金额
        if (request.getAdjustmentAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("调整金额不能为0");
        }

        // 创建调整记录
        StatementAdjustment adjustment = new StatementAdjustment();
        adjustment.setStatementId(request.getStatementId());
        adjustment.setCustomerId(request.getCustomerId());
        adjustment.setCustomerName(customer.getName());
        adjustment.setAdjustmentDate(request.getAdjustmentDate());
        adjustment.setAdjustmentAmount(request.getAdjustmentAmount());
        adjustment.setAdjustmentType(request.getAdjustmentType());
        adjustment.setReason(request.getReason());
        adjustment.setRemark(request.getRemark());
        adjustment.setOperator(user.userId());
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
                                                                                   LocalDate endDate) {
        List<StatementAdjustment> adjustments = adjustmentMapper.getByDateRange(startDate, endDate)
                .stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .collect(java.util.stream.Collectors.toList());

        return adjustments.stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指定对账单的调整记录
     */
    public List<ReceivableDtos.StatementAdjustmentResponse> getAdjustmentsByStatement(Long statementId) {
        List<StatementAdjustment> adjustments = adjustmentMapper.getByStatementId(statementId);
        return adjustments.stream()
                .map(this::convertToResponse)
                .collect(java.util.stream.Collectors.toList());
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
    public List<ReceivableDtos.AdjustmentStatisticsResponse> getAdjustmentStatistics(LocalDate startDate, LocalDate endDate) {
        List<StatementAdjustmentMapper.AdjustmentStatistics> statistics = adjustmentMapper.getAdjustmentStatistics(startDate, endDate);

        return statistics.stream()
                .map(stat -> new ReceivableDtos.AdjustmentStatisticsResponse(
                        stat.getAdjustmentType(),
                        stat.getCount(),
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
        // 获取客户信息
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        // 查询指定日期范围内的应收账款
        List<Receivable> receivables = receivableMapper.getByDateRange(startDate, endDate);

        // 计算期初余额（取上一天的数据）
        LocalDate prevDate = startDate.minusDays(1);
        List<Receivable> prevReceivables = receivableMapper.getByDateRange(prevDate, prevDate);
        BigDecimal openingBalance = prevReceivables.stream()
                .map(Receivable::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算本期数据
        BigDecimal currentReceivables = receivables.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal payments = receivables.stream()
                .map(Receivable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取调整金额
        BigDecimal adjustments = adjustmentMapper.getByDateRange(startDate, endDate)
                .stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .map(StatementAdjustment::getAdjustmentAmount)
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

        // 添加调整明细
        List<StatementAdjustment> adjustmentList = adjustmentMapper.getByDateRange(startDate, endDate)
                .stream()
                .filter(a -> a.getCustomerId().equals(customerId))
                .collect(java.util.stream.Collectors.toList());

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