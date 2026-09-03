package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.service.ReceivableService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

/**
 * 账龄管理Controller
 */
@RestController
@RequestMapping("/finance/aging")
@RequiredArgsConstructor
public class AgingController {

    private final AgingUpdateService agingUpdateService;
    private final ReceivableService receivableService;

    /**
     * 手动更新指定应收账款的账龄
     */
    @PutMapping("/receivable/{receivableId}")
    public Result<Void> updateAgingForReceivable(@PathVariable Long receivableId) {
        agingUpdateService.updateAgingForReceivable(receivableId);
        return Result.ok();
    }

    /**
     * 批量更新指定客户的应收账款账龄
     */
    @PutMapping("/customer/{customerId}")
    public Result<Void> updateAgingForCustomer(@PathVariable Long customerId) {
        agingUpdateService.updateAgingForCustomer(customerId);
        return Result.ok();
    }

    /**
     * 获取账龄统计信息
     */
    @GetMapping("/statistics")
    public Result<AgingUpdateService.AgingStatistics> getAgingStatistics() {
        AgingUpdateService.AgingStatistics statistics = agingUpdateService.getAgingStatistics();
        return Result.ok(statistics);
    }

    /**
     * 获取账龄分析数据
     */
    @GetMapping("/analysis")
    public Result<java.util.List<ReceivableDtos.AgingAnalysisResponse>> getAgingAnalysis() {
        return Result.ok(receivableService.getAgingAnalysis());
    }

    /**
     * 获取催收建议
     */
    @GetMapping("/collection-advice")
    public Result<String> getCollectionAdvice() {
        // 根据账龄分析结果生成催收建议
        StringBuilder advice = new StringBuilder();

        // 获取账龄统计
        AgingUpdateService.AgingStatistics statistics = agingUpdateService.getAgingStatistics();

        // 生成建议
        if (statistics.getBucketOver90Count() > 0) {
            advice.append("【紧急】有 ").append(statistics.getBucketOver90Count())
                  .append(" 笔应收账款逾期超过90天，建议立即采取法律行动。\n");
        }

        if (statistics.getBucket61_90Count() > 0) {
            advice.append("【重要】有 ").append(statistics.getBucket61_90Count())
                  .append(" 笔应收账款逾期61-90天，建议加大催收力度。\n");
        }

        if (statistics.getBucket31_60Count() > 0) {
            advice.append("【关注】有 ").append(statistics.getBucket31_60Count())
                  .append(" 笔应收账款逾期31-60天，建议定期跟进。\n");
        }

        if (statistics.getBucket1_30Count() > 0) {
            advice.append("【提醒】有 ").append(statistics.getBucket1_30Count())
                  .append(" 笔应收账款逾期1-30天，建议及时提醒客户。\n");
        }

        if (statistics.getUnsettledCount() > 0) {
            advice.append("【预提醒】有 ").append(statistics.getUnsettledCount())
                  .append(" 笔应收账款即将到期，建议提前沟通确认。\n");
        }

        advice.append("\n总应收账款：").append(statistics.getTotalAmount())
              .append(" 元，总计 ").append(statistics.getTotalCount()).append(" 笔。");

        return Result.ok(advice.toString());
    }

    /**
     * 获取账龄预警
     */
    @GetMapping("/warnings")
    public Result<String> getAgingWarnings() {
        StringBuilder warnings = new StringBuilder();

        AgingUpdateService.AgingStatistics statistics = agingUpdateService.getAgingStatistics();

        // 设置预警阈值
        int highRiskThreshold = 5; // 高风险客户数量阈值
        BigDecimal highAmountThreshold = new java.math.BigDecimal("50000"); // 高风险金额阈值

        // 检查高风险客户
        if (statistics.getBucketOver90Count() >= highRiskThreshold ||
            statistics.getBucketOver90Amount().compareTo(highAmountThreshold) >= 0) {
            warnings.append("【高风险预警】存在大量长期逾期应收账款，可能存在坏账风险。\n");
        }

        // 检查逾期比例
        if (statistics.getTotalCount() > 0) {
            double overdueRatio = (double) (statistics.getTotalCount() - statistics.getUnsettledCount())
                               / statistics.getTotalCount();
            if (overdueRatio > 0.3) { // 逾期超过30%
                warnings.append("【逾期率过高】应收账款逾期率达到 ").append(String.format("%.1f%%", overdueRatio * 100))
                      .append("，建议加强信用管理。\n");
            }
        }

        // 检查大额逾期
        if (statistics.getBucketOver90Amount().compareTo(highAmountThreshold) >= 0) {
            warnings.append("【大额逾期】存在").append(statistics.getBucketOver90Amount())
                  .append("元应收账款逾期超过90天，建议重点关注。\n");
        }

        if (warnings.length() == 0) {
            warnings.append("当前应收账款账龄正常，无预警信息。");
        }

        return Result.ok(warnings.toString());
    }
}