package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.finance.dto.PayableDtos;
import com.erp.module.finance.entity.Payable;
import com.erp.module.finance.mapper.PayableMapper;
import com.erp.module.masterdata.entity.Supplier;
import com.erp.module.masterdata.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PayableService {
    private static final List<String> AGING_BUCKETS = List.of("未到期", "1-30天", "31-60天", "61-90天", "90天以上");

    private final PayableMapper payableMapper;
    private final SupplierMapper supplierMapper;

    public PageResult<PayableDtos.ListResponse> list(Long supplierId, String status,
                                                       LocalDate startDate, LocalDate endDate,
                                                       LocalDate dueStartDate, LocalDate dueEndDate,
                                                       long page, long size) {
        LambdaQueryWrapper<Payable> query = payableQuery(supplierId, status, startDate, endDate,
                dueStartDate, dueEndDate);
        long total = payableMapper.selectCount(query);
        query.orderByAsc(Payable::getDueDate).orderByDesc(Payable::getId);
        long offset = Math.max(page - 1, 0) * Math.max(size, 1);
        query.last("OFFSET " + offset + " ROWS FETCH NEXT " + Math.max(size, 1) + " ROWS ONLY");
        List<PayableDtos.ListResponse> records = payableMapper.selectList(query).stream()
                .map(this::toResponse)
                .toList();
        return new PageResult<>(total, records);
    }

    public PayableDtos.ListResponse detail(Long id) {
        Payable payable = payableMapper.selectById(id);
        if (payable == null) {
            throw new BusinessException("应付账款不存在");
        }
        return toResponse(payable);
    }

    public List<PayableDtos.AgingResponse> aging(Long supplierId) {
        Map<String, PayableDtos.AgingResponse> result = new LinkedHashMap<>();
        for (String bucket : AGING_BUCKETS) {
            PayableDtos.AgingResponse item = new PayableDtos.AgingResponse();
            item.setBucket(bucket);
            result.put(bucket, item);
        }
        LambdaQueryWrapper<Payable> query = new LambdaQueryWrapper<Payable>()
                .eq(supplierId != null, Payable::getSupplierId, supplierId)
                .in(Payable::getStatus, "UNSETTLED", "PARTIAL");
        for (Payable payable : payableMapper.selectList(query)) {
            PayableDtos.ListResponse row = toResponse(payable);
            PayableDtos.AgingResponse item = result.get(row.getAgingBucket());
            item.setCount(item.getCount() + 1);
            item.setAmount(item.getAmount().add(row.getAmount()));
            item.setPaidAmount(item.getPaidAmount().add(row.getPaidAmount()));
            item.setRemainingAmount(item.getRemainingAmount().add(row.getRemainingAmount()));
        }
        return new ArrayList<>(result.values());
    }

    private LambdaQueryWrapper<Payable> payableQuery(Long supplierId, String status,
                                                       LocalDate startDate, LocalDate endDate,
                                                       LocalDate dueStartDate, LocalDate dueEndDate) {
        return new LambdaQueryWrapper<Payable>()
                .eq(supplierId != null, Payable::getSupplierId, supplierId)
                .eq(StringUtils.hasText(status), Payable::getStatus, status)
                .ge(startDate != null, Payable::getBizDate, startDate)
                .le(endDate != null, Payable::getBizDate, endDate)
                .ge(dueStartDate != null, Payable::getDueDate, dueStartDate)
                .le(dueEndDate != null, Payable::getDueDate, dueEndDate);
    }

    private PayableDtos.ListResponse toResponse(Payable payable) {
        PayableDtos.ListResponse response = new PayableDtos.ListResponse();
        response.setId(payable.getId());
        response.setSupplierId(payable.getSupplierId());
        Supplier supplier = payable.getSupplierId() == null ? null : supplierMapper.selectById(payable.getSupplierId());
        response.setSupplierName(supplier == null ? "" : supplier.getName());
        response.setDocType(payable.getDocType());
        response.setDocId(payable.getDocId());
        response.setDocNo(payable.getDocNo());
        response.setBizDate(payable.getBizDate());
        response.setDueDate(payable.getDueDate());
        BigDecimal amount = payable.getAmount() == null ? BigDecimal.ZERO : payable.getAmount();
        BigDecimal paidAmount = payable.getPaidAmount() == null ? BigDecimal.ZERO : payable.getPaidAmount();
        response.setAmount(amount);
        response.setPaidAmount(paidAmount);
        response.setRemainingAmount(amount.subtract(paidAmount));
        response.setStatus(payable.getStatus());
        response.setCreatedAt(payable.getCreatedAt());

        long days = payable.getDueDate() == null ? 0 : ChronoUnit.DAYS.between(payable.getDueDate(), LocalDate.now());
        response.setDaysOverdue((int) Math.max(days, 0));
        response.setAgingBucket(days < 0 ? "未到期"
                : days <= 30 ? "1-30天"
                : days <= 60 ? "31-60天"
                : days <= 90 ? "61-90天" : "90天以上");
        return response;
    }
}
