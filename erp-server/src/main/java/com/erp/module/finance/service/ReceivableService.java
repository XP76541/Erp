package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.finance.entity.Payment;
import com.erp.module.finance.entity.PaymentAllocation;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.PaymentMapper;
import com.erp.module.finance.mapper.PaymentAllocationMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.DocSequenceService;
import com.erp.module.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应收账款服务
 */
@Service
public class ReceivableService {

    private final ReceivableMapper receivableMapper;
    private final PaymentMapper paymentMapper;
    private final PaymentAllocationMapper allocationMapper;
    private final CustomerMapper customerMapper;
    private final DocSequenceService docSequenceService;
    private final OperationLogService operationLogService;

    public ReceivableService(ReceivableMapper receivableMapper,
                            PaymentMapper paymentMapper,
                            PaymentAllocationMapper allocationMapper,
                            CustomerMapper customerMapper,
                            DocSequenceService docSequenceService,
                            OperationLogService operationLogService) {
        this.receivableMapper = receivableMapper;
        this.paymentMapper = paymentMapper;
        this.allocationMapper = allocationMapper;
        this.customerMapper = customerMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
    }

    /**
     * 获取应收账款列表
     */
    public PageResult<ReceivableDtos.ReceivableListResponse> getReceivables(ReceivableDtos.ReceivableListRequest params) {
        var wrapper = Wrappers.<Receivable>lambdaQuery();
        if (params.getCustomerId() != null) {
            wrapper.eq(Receivable::getCustomerId, params.getCustomerId());
        }
        if (params.getStatus() != null) {
            wrapper.eq(Receivable::getStatus, params.getStatus());
        }
        if (params.getStartDate() != null) {
            wrapper.ge(Receivable::getBusinessDate, params.getStartDate());
        }
        if (params.getEndDate() != null) {
            wrapper.le(Receivable::getBusinessDate, params.getEndDate());
        }
        wrapper.orderByDesc(Receivable::getId);

        List<Receivable> receivables = receivableMapper.selectList(wrapper);
        List<ReceivableDtos.ReceivableListResponse> responses = receivables.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return PageResult.of(receivables.size(), responses);
    }

    /**
     * 获取应收账款详情
     */
    public ReceivableDtos.ReceivableListResponse getReceivableDetail(Long id) {
        Receivable receivable = receivableMapper.selectById(id);
        if (receivable == null) {
            throw new BusinessException("应收账款记录不存在");
        }
        return convertToListResponse(receivable);
    }

    /**
     * 创建应收账款
     */
    @Transactional
    public Long createReceivable(ReceivableDtos.ReceivableCreateRequest request, TokenStore.LoginUser user) {
        // 验证客户
        Customer customer = customerMapper.selectById(request.getCustomerId());
        if (customer == null || customer.getIsActive() == 0) {
            throw new BusinessException("客户不存在或已停用");
        }

        LocalDate bizDate = request.getBusinessDate() != null ?
                request.getBusinessDate() : LocalDate.now();

        // 创建应收账款记录
        for (ReceivableDtos.ReceivableCreateRequest.Item item : request.getItems()) {
            Receivable receivable = new Receivable();
            receivable.setDocNo(docSequenceService.nextDocNo("REC", "REC", bizDate.toString()));
            receivable.setOrderId(item.getOrderId());
            receivable.setCustomerId(request.getCustomerId());
            receivable.setCustomerName(customer.getName());
            receivable.setBusinessDate(bizDate);
            receivable.setDueDate(bizDate.plusDays(customer.getPaymentTermDays() != null ?
                    customer.getPaymentTermDays() : 0));
            receivable.setAmount(item.getAmount());
            receivable.setPaidAmount(BigDecimal.ZERO);
            receivable.setRemainingAmount(item.getAmount());
            receivable.setStatus("UNSETTLED");
            receivable.setCreatedAt(LocalDateTime.now());
            receivable.setUpdatedAt(LocalDateTime.now());

            receivableMapper.insert(receivable);
        }

        return 0L; // 返回创建的记录数量
    }

    /**
     * 收款核销
     */
    @Transactional
    public ReceivableDtos.SettleResponse settleReceivable(ReceivableDtos.SettleRequest request,
                                                        TokenStore.LoginUser user) {
        Receivable receivable = receivableMapper.selectById(request.getReceivableId());
        if (receivable == null) {
            throw new BusinessException("应收账款记录不存在");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("核销金额必须大于0");
        }

        if (request.getAmount().compareTo(receivable.getRemainingAmount()) > 0) {
            throw new BusinessException("核销金额不能超过剩余金额");
        }

        // 创建收款单
        Payment payment = new Payment();
        payment.setDocNo(docSequenceService.nextDocNo("PAY", "PAY", LocalDate.now().toString()));
        payment.setCustomerId(receivable.getCustomerId());
        payment.setCustomerName(receivable.getCustomerName());
        payment.setBusinessDate(LocalDate.now());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setStatus("UNALLOCATED");
        payment.setCreatedAt(LocalDateTime.now());

        paymentMapper.insert(payment);

        // 创建核销记录
        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setPaymentId(payment.getId());
        allocation.setReceivableId(receivable.getId());
        allocation.setAllocatedAmount(request.getAmount());
        allocation.setCreatedAt(LocalDateTime.now());

        allocationMapper.insert(allocation);

        int updated = receivableMapper.updatePaidAmount(receivable.getId(), request.getAmount());
        if (updated != 1) {
            throw new BusinessException("应收账款已被其他收款更新，请刷新后重试");
        }

        // 更新收款单状态
        payment.setAllocatedAmount(request.getAmount());
        if (request.getAmount().compareTo(payment.getAmount()) == 0) {
            payment.setStatus("FULLY_ALLOCATED");
        } else {
            payment.setStatus("PARTIALLY_ALLOCATED");
        }
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 记录操作日志
        operationLogService.record(user, "receivable", "SETTLE",
                "RECEIVABLE", receivable.getId(), receivable.getDocNo(),
                "{\"amount\":" + request.getAmount() + "}", null);

        return new ReceivableDtos.SettleResponse(payment.getId(), payment.getDocNo(),
                request.getAmount(), payment.getStatus(), "核销成功");
    }

    /**
     * 批量核销
     */
    @Transactional
    public void batchSettle(ReceivableDtos.BatchSettleRequest request, TokenStore.LoginUser user) {
        for (ReceivableDtos.SettleRequest settleRequest : request.getSettlements()) {
            settleReceivable(settleRequest, user);
        }
    }

    /**
     * 获取客户应收账款统计
     */
    public List<ReceivableDtos.ReceivableStatisticsResponse> getCustomerStatistics() {
        List<ReceivableMapper.ReceivableStatistics> statistics = receivableMapper.getCustomerReceivableStatistics();
        return statistics.stream()
                .map(stat -> {
                    Customer customer = customerMapper.selectById(stat.getCustomerId());
                    return new ReceivableDtos.ReceivableStatisticsResponse(
                            stat.getCustomerId(),
                            customer != null ? customer.getName() : "客户" + stat.getCustomerId(),
                            stat.getTotalAmount(),
                            stat.getTotalPaid(),
                            stat.getTotalRemaining(),
                            stat.getUnsettledAmount(),
                            stat.getPartialAmount(),
                            stat.getSettledAmount());
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取账龄分析
     */
    public List<ReceivableDtos.AgingAnalysisResponse> getAgingAnalysis() {
        List<ReceivableMapper.AgingAnalysis> analysis = receivableMapper.getAgingAnalysis();
        return analysis.stream()
                .map(analysisData -> new ReceivableDtos.AgingAnalysisResponse(
                        analysisData.getAgingBucket(),
                        analysisData.getTotalAmount(),
                        analysisData.getTotalPaid(),
                        analysisData.getTotalRemaining()))
                .collect(Collectors.toList());
    }

    /**
     * 获取逾期应收账款
     */
    public List<ReceivableDtos.ReceivableListResponse> getOverdueReceivables() {
        List<Receivable> overdueReceivables = receivableMapper.getOverdueReceivables();
        return overdueReceivables.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取客户应收账款汇总
     */
    public ReceivableDtos.CustomerReceivableSummary getCustomerSummary(Long customerId) {
        // 获取客户基本信息
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        // 查询该客户的所有应收账款
        List<Receivable> receivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .eq(Receivable::getCustomerId, customerId));

        // 计算汇总数据
        BigDecimal totalReceivable = receivables.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = receivables.stream()
                .map(Receivable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOverdue = receivables.stream()
                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 0)
                .map(Receivable::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer countUnsettled = (int) receivables.stream()
                .filter(r -> "UNSETTLED".equals(r.getStatus()))
                .count();

        Integer countOverdue = (int) receivables.stream()
                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 0)
                .count();

        // 账龄分布
        List<ReceivableDtos.CustomerReceivableSummary.AgingDistribution> agingDistribution = List.of(
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("未到期",
                        receivables.stream()
                                .filter(r -> r.getDaysOverdue() == null || r.getDaysOverdue() <= 0)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> r.getDaysOverdue() == null || r.getDaysOverdue() <= 0)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("1-30天",
                        receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 0 && r.getDaysOverdue() <= 30)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 0 && r.getDaysOverdue() <= 30)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("31-60天",
                        receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 30 && r.getDaysOverdue() <= 60)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 30 && r.getDaysOverdue() <= 60)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("61-90天",
                        receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 60 && r.getDaysOverdue() <= 90)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 60 && r.getDaysOverdue() <= 90)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("90天以上",
                        receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 90)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> r.getDaysOverdue() != null && r.getDaysOverdue() > 90)
                                .count())
        );

        return new ReceivableDtos.CustomerReceivableSummary(
                customerId,
                customer.getName(),
                totalReceivable,
                totalPaid,
                totalOverdue,
                countUnsettled,
                countOverdue,
                agingDistribution);
    }

    /**
     * 生成对账单
     */
    public ReceivableDtos.StatementResponse generateStatement(Long customerId,
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

        BigDecimal adjustments = BigDecimal.ZERO; // TODO: 添加调整金额的逻辑

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
                .collect(Collectors.toList());

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
    private ReceivableDtos.ReceivableListResponse convertToListResponse(Receivable receivable) {
        ReceivableDtos.ReceivableListResponse response = new ReceivableDtos.ReceivableListResponse();
        response.setId(receivable.getId());
        response.setDocNo(receivable.getDocNo());
        response.setOrderDocNo(receivable.getOrderDocNo());
        response.setCustomerId(receivable.getCustomerId());
        response.setCustomerName(receivable.getCustomerName());
        response.setBusinessDate(receivable.getBusinessDate());
        response.setDueDate(receivable.getDueDate());
        response.setAmount(receivable.getAmount());
        response.setPaidAmount(receivable.getPaidAmount());
        response.setRemainingAmount(receivable.getRemainingAmount());
        response.setStatus(receivable.getStatus());
        response.setDaysOverdue(receivable.getDaysOverdue());
        response.setAgingBucket(receivable.getAgingBucket());
        response.setCreatedAt(receivable.getCreatedAt().toString());
        return response;
    }
}