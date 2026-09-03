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
import com.erp.module.system.service.SystemAuthorizationService;
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
    private final SystemAuthorizationService authorizationService;

    public ReceivableService(ReceivableMapper receivableMapper,
                            PaymentMapper paymentMapper,
                            PaymentAllocationMapper allocationMapper,
                            CustomerMapper customerMapper,
                            DocSequenceService docSequenceService,
                            OperationLogService operationLogService,
                            SystemAuthorizationService authorizationService) {
        this.receivableMapper = receivableMapper;
        this.paymentMapper = paymentMapper;
        this.allocationMapper = allocationMapper;
        this.customerMapper = customerMapper;
        this.docSequenceService = docSequenceService;
        this.operationLogService = operationLogService;
        this.authorizationService = authorizationService;
    }

    /**
     * 获取应收账款列表
     */
    public PageResult<ReceivableDtos.ReceivableListResponse> getReceivables(ReceivableDtos.ReceivableListRequest params) {
        return getReceivables(params, null);
    }

    public PageResult<ReceivableDtos.ReceivableListResponse> getReceivables(ReceivableDtos.ReceivableListRequest params,
                                                                              TokenStore.LoginUser user) {
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
        Long scope = user == null ? null : authorizationService.salespersonScope(user);
        if (scope != null) {
            List<Long> customerIds = customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                            .eq(Customer::getSalespersonId, scope))
                    .stream().map(Customer::getId).toList();
            if (customerIds.isEmpty()) return PageResult.of(0, List.of());
            wrapper.in(Receivable::getCustomerId, customerIds);
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
        return getReceivableDetail(id, null);
    }

    public ReceivableDtos.ReceivableListResponse getReceivableDetail(Long id, TokenStore.LoginUser user) {
        Receivable receivable = receivableMapper.selectById(id);
        if (receivable == null) {
            throw new BusinessException("应收账款记录不存在");
        }
        if (user != null) {
            Customer customer = customerMapper.selectById(receivable.getCustomerId());
            if (customer != null) authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
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

        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
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
        Customer customer = customerMapper.selectById(receivable.getCustomerId());
        if (customer != null) authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

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
        return getCustomerStatistics(null);
    }

    public List<ReceivableDtos.ReceivableStatisticsResponse> getCustomerStatistics(TokenStore.LoginUser user) {
        List<Long> customerIds = scopedCustomerIds(user);
        if (customerIds != null && customerIds.isEmpty()) return List.of();
        List<ReceivableMapper.ReceivableStatistics> statistics = receivableMapper.getCustomerReceivableStatistics();
        return statistics.stream()
                .filter(stat -> customerIds == null || customerIds.contains(stat.getCustomerId()))
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

    /** 获取指定截止日期的账龄分析。 */
    public List<ReceivableDtos.AgingAnalysisResponse> getAgingAnalysis() {
        return getAgingAnalysis(LocalDate.now(), null);
    }

    public List<ReceivableDtos.AgingAnalysisResponse> getAgingAnalysis(LocalDate cutoffDate,
                                                                        TokenStore.LoginUser user) {
        LocalDate cutoff = cutoffDate == null ? LocalDate.now() : cutoffDate;
        List<Long> customerIds = scopedCustomerIds(user);
        if (customerIds != null && customerIds.isEmpty()) return List.of();
        return receivableMapper.getAgingAnalysis(cutoff, customerIds).stream()
                .map(data -> new ReceivableDtos.AgingAnalysisResponse(
                        data.getAgingBucket(), data.getTotalAmount(),
                        data.getTotalPaid(), data.getTotalRemaining()))
                .collect(Collectors.toList());
    }

    /** 获取指定截止日期的逾期应收账款。 */
    public List<ReceivableDtos.ReceivableListResponse> getOverdueReceivables() {
        return getOverdueReceivables(LocalDate.now(), null);
    }

    public List<ReceivableDtos.ReceivableListResponse> getOverdueReceivables(LocalDate cutoffDate,
                                                                               TokenStore.LoginUser user) {
        LocalDate cutoff = cutoffDate == null ? LocalDate.now() : cutoffDate;
        List<Long> customerIds = scopedCustomerIds(user);
        if (customerIds != null && customerIds.isEmpty()) return List.of();
        return receivableMapper.getOverdueReceivables(cutoff, customerIds).stream()
                .map(receivable -> convertToListResponse(receivable, cutoff))
                .collect(Collectors.toList());
    }

    /**
     * 获取客户应收账款汇总
     */
    public ReceivableDtos.CustomerReceivableSummary getCustomerSummary(Long customerId) {
        return getCustomerSummary(customerId, null);
    }

    public ReceivableDtos.CustomerReceivableSummary getCustomerSummary(Long customerId, TokenStore.LoginUser user) {
        // 获取客户基本信息
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        if (user != null) authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        LocalDate asOfDate = LocalDate.now();
        List<Receivable> receivables = receivablesForCustomer(customerId);

        java.util.function.Function<Receivable, Integer> overdueDays = r ->
                r.getDueDate() != null && r.getDueDate().isBefore(asOfDate)
                        ? (int) ChronoUnit.DAYS.between(r.getDueDate(), asOfDate) : 0;

        // 计算汇总数据
        BigDecimal totalReceivable = receivables.stream()
                .map(Receivable::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = receivables.stream()
                .map(Receivable::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOverdue = receivables.stream()
                .filter(r -> overdueDays.apply(r) > 0)
                .map(Receivable::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer countUnsettled = (int) receivables.stream()
                .filter(r -> "UNSETTLED".equals(r.getStatus()))
                .count();

        Integer countOverdue = (int) receivables.stream()
                .filter(r -> overdueDays.apply(r) > 0)
                .count();

        // 账龄分布
        List<ReceivableDtos.CustomerReceivableSummary.AgingDistribution> agingDistribution = List.of(
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("未到期",
                        receivables.stream()
                .filter(r -> overdueDays.apply(r) <= 0)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                .filter(r -> overdueDays.apply(r) <= 0)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("1-30天",
                        receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 0 && overdueDays.apply(r) <= 30)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 0 && overdueDays.apply(r) <= 30)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("31-60天",
                        receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 30 && overdueDays.apply(r) <= 60)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 30 && overdueDays.apply(r) <= 60)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("61-90天",
                        receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 60 && overdueDays.apply(r) <= 90)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 60 && overdueDays.apply(r) <= 90)
                                .count()),
                new ReceivableDtos.CustomerReceivableSummary.AgingDistribution("90天以上",
                        receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 90)
                                .map(Receivable::getRemainingAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (int) receivables.stream()
                                .filter(r -> overdueDays.apply(r) > 90)
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

    private List<Long> scopedCustomerIds(TokenStore.LoginUser user) {
        Long scope = user == null ? null : authorizationService.salespersonScope(user);
        if (scope == null) return null;
        return customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                        .eq(Customer::getSalespersonId, scope))
                .stream().map(Customer::getId).toList();
    }

    private List<Receivable> receivablesForCustomer(Long customerId) {
        return receivableMapper.selectList(Wrappers.<Receivable>lambdaQuery()
                .eq(Receivable::getCustomerId, customerId));
    }

    private ReceivableDtos.ReceivableListResponse convertToListResponse(Receivable receivable) {
        return convertToListResponse(receivable, LocalDate.now());
    }

    private ReceivableDtos.ReceivableListResponse convertToListResponse(Receivable receivable, LocalDate asOfDate) {
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
        int daysOverdue = receivable.getDueDate() != null && receivable.getDueDate().isBefore(asOfDate)
                ? (int) ChronoUnit.DAYS.between(receivable.getDueDate(), asOfDate) : 0;
        response.setDaysOverdue(daysOverdue);
        response.setAgingBucket(daysOverdue <= 0 ? "未到期" : daysOverdue <= 30 ? "1-30天"
                : daysOverdue <= 60 ? "31-60天" : daysOverdue <= 90 ? "61-90天" : "90天以上");
        response.setCreatedAt(receivable.getCreatedAt() == null ? null : receivable.getCreatedAt().toString());
        return response;
    }
}