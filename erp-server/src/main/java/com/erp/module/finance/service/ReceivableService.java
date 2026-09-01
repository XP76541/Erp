package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.PageResult;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.finance.dto.ReceivableDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableMapper receivableMapper;

    public PageResult<ReceivableDtos.ReceivableListResponse> getReceivables(ReceivableDtos.ReceivableListRequest params) {
        QueryWrapper<Receivable> wrapper = new QueryWrapper<>();
        if (params.getCustomerId() != null) {
            wrapper.eq("customer_id", params.getCustomerId());
        }
        if (params.getStatus() != null) {
            wrapper.eq("status", params.getStatus());
        }
        if (params.getStartDate() != null) {
            wrapper.ge("business_date", params.getStartDate());
        }
        if (params.getEndDate() != null) {
            wrapper.le("business_date", params.getEndDate());
        }
        wrapper.orderByDesc("created_at");

        List<Receivable> receivables = receivableMapper.selectList(wrapper);
        List<ReceivableDtos.ReceivableListResponse> responses = receivables.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());

        return new PageResult<>(responses, receivables.size());
    }

    public List<ReceivableDtos.ReceivableStatisticsResponse> getCustomerStatistics() {
        List<Receivable.ReceivableStatistics> statistics = receivableMapper.getCustomerReceivableStatistics();
        return statistics.stream()
                .map(stat -> {
                    ReceivableDtos.ReceivableStatisticsResponse response = new ReceivableDtos.ReceivableStatisticsResponse();
                    response.setCustomerId(stat.getCustomerId());
                    response.setCustomerName(getCustomerName(stat.getCustomerId()));
                    response.setTotalAmount(stat.getTotalAmount());
                    response.setTotalPaid(stat.getTotalPaid());
                    response.setTotalRemaining(stat.getTotalRemaining());
                    response.setUnsettledAmount(stat.getUnsettledAmount());
                    response.setPartialAmount(stat.getPartialAmount());
                    response.setSettledAmount(stat.getSettledAmount());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<ReceivableDtos.AgingAnalysisResponse> getAgingAnalysis() {
        List<Receivable.AgingAnalysis> analysis = receivableMapper.getAgingAnalysis();
        return analysis.stream()
                .map(analysisData -> {
                    ReceivableDtos.AgingAnalysisResponse response = new ReceivableDtos.AgingAnalysisResponse();
                    response.setAgingBucket(analysisData.getAgingBucket());
                    response.setTotalAmount(analysisData.getTotalAmount());
                    response.setTotalPaid(analysisData.getTotalPaid());
                    response.setTotalRemaining(analysisData.getTotalRemaining());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<ReceivableDtos.ReceivableListResponse> getOverdueReceivables() {
        List<Receivable> overdueReceivables = receivableMapper.getOverdueReceivables();
        return overdueReceivables.stream()
                .map(this::convertToListResponse)
                .collect(Collectors.toList());
    }

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

        // 计算逾期天数
        if (receivable.getDueDate() != null) {
            long daysOverdue = ChronoUnit.DAYS.between(receivable.getDueDate(), LocalDate.now());
            response.setDaysOverdue(daysOverdue > 0 ? (int) daysOverdue : 0);

            // 设置账龄分类
            if (daysOverdue < 0) {
                response.setAgingBucket("未到期");
            } else if (daysOverdue <= 30) {
                response.setAgingBucket("1-30天");
            } else if (daysOverdue <= 60) {
                response.setAgingBucket("31-60天");
            } else if (daysOverdue <= 90) {
                response.setAgingBucket("61-90天");
            } else {
                response.setAgingBucket("90天以上");
            }
        } else {
            response.setDaysOverdue(0);
            response.setAgingBucket("未确定");
        }

        response.setCreatedAt(receivable.getCreatedAt().toString());
        return response;
    }

    private String getCustomerName(Long customerId) {
        // TODO: 从客户服务获取客户名称
        return "客户" + customerId;
    }
}