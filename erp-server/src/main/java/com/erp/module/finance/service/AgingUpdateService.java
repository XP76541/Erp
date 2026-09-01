package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.ReceivableMapper;
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
                // 计算逾期天数
                LocalDate dueDate = receivable.getDueDate();
                LocalDate currentDate = LocalDate.now();

                if (dueDate != null && !dueDate.isAfter(currentDate)) {
                    int daysOverdue = (int) ChronoUnit.DAYS.between(dueDate, currentDate);
                    receivable.setDaysOverdue(daysOverdue);

                    // 更新账龄分类
                    String agingBucket = calculateAgingBucket(daysOverdue);
                    receivable.setAgingBucket(agingBucket);

                    // 如果剩余金额为0，更新状态为已结算
                    if (receivable.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0) {
                        receivable.setStatus("SETTLED");
                    }

                    // 更新记录
                    receivableMapper.updateById(receivable);
                    updatedCount++;
                }
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
        Receivable receivable = receivableMapper.selectById(receivableId);
        if (receivable == null) {
            throw new IllegalArgumentException("应收账款记录不存在");
        }

        // 计算逾期天数
        LocalDate dueDate = receivable.getDueDate();
        LocalDate currentDate = LocalDate.now();

        if (dueDate != null && !dueDate.isAfter(currentDate)) {
            int daysOverdue = (int) ChronoUnit.DAYS.between(dueDate, currentDate);
            receivable.setDaysOverdue(daysOverdue);

            // 更新账龄分类
            String agingBucket = calculateAgingBucket(daysOverdue);
            receivable.setAgingBucket(agingBucket);

            // 更新记录
            receivableMapper.updateById(receivable);

            log.info("更新应收账款 {} 账龄成功，逾期天数：{}，账龄分类：{}",
                    receivable.getDocNo(), daysOverdue, agingBucket);
        }
    }

    /**
     * 批量更新指定客户的应收账款账龄
     */
    @Transactional
    public void updateAgingForCustomer(Long customerId) {
        List<Receivable> receivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .eq(Receivable::getCustomerId, customerId)
                        .ne(Receivable::getStatus, "SETTLED")
        );

        int updatedCount = 0;

        for (Receivable receivable : receivables) {
            updateAgingForReceivable(receivable.getId());
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
        List<Receivable> receivables = receivableMapper.selectList(null);

        AgingStatistics statistics = new AgingStatistics();

        // 统计各账龄区间的金额和数量
        for (Receivable receivable : receivables) {
            if (receivable.getAgingBucket() != null) {
                switch (receivable.getAgingBucket()) {
                    case "未到期":
                        statistics.unsettledAmount = statistics.unsettledAmount.add(
                                receivable.getRemainingAmount());
                        statistics.unsettledCount++;
                        break;
                    case "1-30天":
                        statistics.bucket1_30Amount = statistics.bucket1_30Amount.add(
                                receivable.getRemainingAmount());
                        statistics.bucket1_30Count++;
                        break;
                    case "31-60天":
                        statistics.bucket31_60Amount = statistics.bucket31_60Amount.add(
                                receivable.getRemainingAmount());
                        statistics.bucket31_60Count++;
                        break;
                    case "61-90天":
                        statistics.bucket61_90Amount = statistics.bucket61_90Amount.add(
                                receivable.getRemainingAmount());
                        statistics.bucket61_90Count++;
                        break;
                    case "90天以上":
                        statistics.bucketOver90Amount = statistics.bucketOver90Amount.add(
                                receivable.getRemainingAmount());
                        statistics.bucketOver90Count++;
                        break;
                }
            }
        }

        // 计算总计
        statistics.totalAmount = statistics.unsettledAmount
                .add(statistics.bucket1_30Amount)
                .add(statistics.bucket31_60Amount)
                .add(statistics.bucket61_90Amount)
                .add(statistics.bucketOver90Amount);
        statistics.totalCount = statistics.unsettledCount
                + statistics.bucket1_30Count
                + statistics.bucket31_60Count
                + statistics.bucket61_90Count
                + statistics.bucketOver90Count;

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