package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.SystemAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 应收账款账龄更新服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgingUpdateService {

    private final ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final SystemAuthorizationService authorizationService;

    /**
     * 每天凌晨2点更新账龄
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void updateAgingDaily() {
        log.info("开始更新应收账款账龄...");

        try {
            // 获取所有未完全结算的应收账款
            List<Receivable> receivables = receivableMapper.selectList(
                    Wrappers.<Receivable>lambdaQuery()
                            .ne(Receivable::getStatus, "SETTLED")
            );

            int updatedCount = 0;

            for (Receivable receivable : receivables) {
                // 以当前日期计算账龄；未到期记录也要刷新为“未到期”，避免账龄字段长期为空或过期
                LocalDate dueDate = receivable.getDueDate();
                LocalDate currentDate = LocalDate.now();
                int daysOverdue = dueDate == null || dueDate.isAfter(currentDate)
                        ? -1 : (int) ChronoUnit.DAYS.between(dueDate, currentDate);
                receivable.setDaysOverdue(daysOverdue);
                receivable.setAgingBucket(calculateAgingBucket(daysOverdue));

                // 如果剩余金额为0，更新状态为已结算
                if (receivable.getRemainingAmount() != null
                        && receivable.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
                    receivable.setStatus("SETTLED");
                }

                // 更新记录
                receivableMapper.updateById(receivable);
                updatedCount++;
            }

            log.info("应收账款账龄更新完成，共更新 {} 条记录", updatedCount);
        } catch (Exception e) {
            log.error("更新应收账款账龄失败", e);
            throw new RuntimeException("更新账龄失败", e);
        }
    }

    /**
     * 手动更新指定应收账款的账龄
     */
    @Transactional
    public void updateAgingForReceivable(Long receivableId) {
        updateAgingForReceivable(receivableId, null);
    }

    public void updateAgingForReceivable(Long receivableId, TokenStore.LoginUser user) {
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收账款记录不存在");
        }
        Customer customer = customerMapper.selectById(receivable.getCustomerId());
        if (customer == null) throw new IllegalArgumentException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());

        // 计算逾期天数；未到期记录也写入统一的“未到期”分类
        LocalDate dueDate = receivable.getDueDate();
        LocalDate currentDate = LocalDate.now();
        int daysOverdue = dueDate == null || dueDate.isAfter(currentDate)
                ? -1 : (int) ChronoUnit.DAYS.between(dueDate, currentDate);
        receivable.setDaysOverdue(daysOverdue);
        String agingBucket = calculateAgingBucket(daysOverdue);
        receivable.setAgingBucket(agingBucket);
        if (receivable.getRemainingAmount() != null
                && receivable.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus("SETTLED");
        }
        receivableMapper.updateById(receivable);

        log.info("更新应收账款 {} 账龄成功，逾期天数：{}，账龄分类：{}",
                receivable.getDocNo(), daysOverdue, agingBucket);
    }

    /**
     * 批量更新指定客户的应收账款账龄
     */
    @Transactional
    public void updateAgingForCustomer(Long customerId) {
        updateAgingForCustomer(customerId, null);
    }

    public void updateAgingForCustomer(Long customerId, TokenStore.LoginUser user) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) throw new IllegalArgumentException("客户不存在");
        authorizationService.requireUnrestrictedOrSalesperson(user, customer.getSalespersonId());
        List<Receivable> receivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .eq(Receivable::getCustomerId, customerId)
                        .ne(Receivable::getStatus, "SETTLED")
        );

        int updatedCount = 0;

        for (Receivable receivable : receivables) {
            updateAgingForReceivable(receivable.getId(), user);
            updatedCount++;
        }

        log.info("客户 {} 应收账款账龄更新完成，共更新 {} 条记录", customerId, updatedCount);
    }

    /**
     * 计算账龄分类
     */
    private String calculateAgingBucket(int daysOverdue) {
        if (daysOverdue < 0) {
            return "未到期";
        } else if (daysOverdue <= 30) {
            return "1-30天";
        } else if (daysOverdue <= 60) {
            return "31-60天";
        } else if (daysOverdue <= 90) {
            return "61-90天";
        } else {
            return "90天以上";
        }
    }

    /**
     * 获取账龄统计信息
     */
    public AgingStatistics getAgingStatistics() {
        return getAgingStatistics(LocalDate.now(), null);
    }

    public AgingStatistics getAgingStatistics(LocalDate cutoffDate, TokenStore.LoginUser user) {
        LocalDate cutoff = cutoffDate == null ? LocalDate.now() : cutoffDate;
        Long scope = user == null ? null : authorizationService.salespersonScope(user);
        List<Long> customerIds = scope == null ? null : customerMapper.selectList(
                Wrappers.<Customer>lambdaQuery().eq(Customer::getSalespersonId, scope))
                .stream().map(Customer::getId).toList();
        if (customerIds != null && customerIds.isEmpty()) return new AgingStatistics();
        List<Receivable> receivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery().ne(Receivable::getStatus, "SETTLED")
                        .gt(Receivable::getRemainingAmount, BigDecimal.ZERO)
                        .le(Receivable::getBusinessDate, cutoff)
                        .in(customerIds != null, Receivable::getCustomerId, customerIds));

        AgingStatistics statistics = new AgingStatistics();
        for (Receivable receivable : receivables) {
            int days = receivable.getDueDate() == null || !receivable.getDueDate().isBefore(cutoff)
                    ? -1 : (int) ChronoUnit.DAYS.between(receivable.getDueDate(), cutoff);
            BigDecimal amount = receivable.getRemainingAmount();
            if (days < 0) { statistics.unsettledAmount = statistics.unsettledAmount.add(amount); statistics.unsettledCount++; }
            else if (days <= 30) { statistics.bucket1_30Amount = statistics.bucket1_30Amount.add(amount); statistics.bucket1_30Count++; }
            else if (days <= 60) { statistics.bucket31_60Amount = statistics.bucket31_60Amount.add(amount); statistics.bucket31_60Count++; }
            else if (days <= 90) { statistics.bucket61_90Amount = statistics.bucket61_90Amount.add(amount); statistics.bucket61_90Count++; }
            else { statistics.bucketOver90Amount = statistics.bucketOver90Amount.add(amount); statistics.bucketOver90Count++; }
        }
        statistics.totalAmount = statistics.unsettledAmount.add(statistics.bucket1_30Amount)
                .add(statistics.bucket31_60Amount).add(statistics.bucket61_90Amount).add(statistics.bucketOver90Amount);
        statistics.totalCount = statistics.unsettledCount + statistics.bucket1_30Count + statistics.bucket31_60Count
                + statistics.bucket61_90Count + statistics.bucketOver90Count;
        return statistics;
    }

    /**
     * 账龄统计内部类
     */
    public static class AgingStatistics {
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private int totalCount = 0;

        private BigDecimal unsettledAmount = BigDecimal.ZERO;
        private int unsettledCount = 0;

        private BigDecimal bucket1_30Amount = BigDecimal.ZERO;
        private int bucket1_30Count = 0;

        private BigDecimal bucket31_60Amount = BigDecimal.ZERO;
        private int bucket31_60Count = 0;

        private BigDecimal bucket61_90Amount = BigDecimal.ZERO;
        private int bucket61_90Count = 0;

        private BigDecimal bucketOver90Amount = BigDecimal.ZERO;
        private int bucketOver90Count = 0;

        // Getters
        public BigDecimal getTotalAmount() { return totalAmount; }
        public int getTotalCount() { return totalCount; }
        public BigDecimal getUnsettledAmount() { return unsettledAmount; }
        public int getUnsettledCount() { return unsettledCount; }
        public BigDecimal getBucket1_30Amount() { return bucket1_30Amount; }
        public int getBucket1_30Count() { return bucket1_30Count; }
        public BigDecimal getBucket31_60Amount() { return bucket31_60Amount; }
        public int getBucket31_60Count() { return bucket31_60Count; }
        public BigDecimal getBucket61_90Amount() { return bucket61_90Amount; }
        public int getBucket61_90Count() { return bucket61_90Count; }
        public BigDecimal getBucketOver90Amount() { return bucketOver90Amount; }
        public int getBucketOver90Count() { return bucketOver90Count; }
    }
}