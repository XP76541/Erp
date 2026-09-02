package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.common.PageResult;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.CollectionRecord;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.service.CollectionService;
import com.erp.module.system.TokenStore;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 催收Controller
 */
@RestController
@RequestMapping("/finance/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    /**
     * 创建催收记录
     */
    @PostMapping
    public Result<Long> create(@RequestBody ReceivableDtos.CollectionRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long id = collectionService.createCollectionRecord(request, currentUser);
        return Result.ok(id);
    }

    /**
     * 获取应收账款的催收记录
     */
    @GetMapping("/receivable/{receivableId}")
    public Result<List<ReceivableDtos.CollectionResponse>> getCollectionRecordsByReceivable(@PathVariable Long receivableId) {
        List<ReceivableDtos.CollectionResponse> records = collectionService.getCollectionRecordsByReceivable(receivableId);
        return Result.ok(records);
    }

    /**
     * 获取客户的催收记录
     */
    @GetMapping("/customer/{customerId}")
    public Result<PageResult<ReceivableDtos.CollectionResponse>> getCollectionRecordsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<ReceivableDtos.CollectionResponse> result = collectionService.getCollectionRecordsByCustomer(customerId, page, size);
        return Result.ok(result);
    }

    /**
     * 更新催收记录
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReceivableDtos.CollectionRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        collectionService.updateCollectionRecord(id, request, currentUser);
        return Result.ok();
    }

    /**
     * 获取催收统计
     */
    @GetMapping("/statistics")
    public Result<ReceivableDtos.CollectionStatsResponse> getStatistics() {
        ReceivableDtos.CollectionStatsResponse statistics = collectionService.getCollectionStatistics();
        return Result.ok(statistics);
    }

    /**
     * 自动生成催收计划
     */
    @GetMapping("/auto-plan")
    public Result<List<ReceivableDtos.CollectionResponse>> generateCollectionPlan() {
        List<Receivable> receivables = collectionService.generateCollectionPlan();
        // 转换为响应格式
        List<ReceivableDtos.CollectionResponse> responses = receivables.stream()
                .map(receivable -> {
                    ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
                    response.setId(receivable.getId());
                    response.setReceivableDocNo(receivable.getDocNo());
                    response.setCustomerId(receivable.getCustomerId());
                    response.setCustomerName(receivable.getCustomerName());
                    response.setAmount(receivable.getRemainingAmount());
                    response.setDaysOverdue(receivable.getDaysOverdue());
                    response.setContactMethod(null);
                    response.setContactResult(null);
                    response.setNextAction(null);
                    response.setOperator(null);
                    response.setCreatedAt(receivable.getCreatedAt().toString());
                    return response;
                }).collect(Collectors.toList());
        return Result.ok(responses);
    }

    /**
     * 批量催收
     */
    @PostMapping("/batch")
    public Result<Void> batchCollection(@RequestBody List<ReceivableDtos.CollectionRequest> requests) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        collectionService.batchCollection(requests, currentUser);
        return Result.ok();
    }

    /**
     * 获取需要升级处理的催收记录
     */
    @GetMapping("/escalation")
    public Result<List<ReceivableDtos.CollectionResponse>> getEscalationRecords() {
        List<CollectionRecord> records = collectionService.getRecordsForEscalation();
        List<ReceivableDtos.CollectionResponse> responses = records.stream()
                .map(record -> {
                    ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
                    response.setId(record.getId());
                    response.setReceivableDocNo(record.getReceivableDocNo());
                    response.setCustomerId(record.getCustomerId());
                    response.setCustomerName(record.getCustomerName());
                    response.setAmount(record.getAmount());
                    response.setDaysOverdue(null);
                    response.setContactMethod(record.getContactMethod());
                    response.setContactResult(record.getContactResult());
                    response.setNextAction(record.getNextAction());
                    response.setOperator(record.getOperatorName());
                    response.setCreatedAt(record.getCreatedAt().toString());
                    return response;
                }).collect(Collectors.toList());
        return Result.ok(responses);
    }

    /**
     * 获取催收记录详情
     */
    @GetMapping("/{id}")
    public Result<ReceivableDtos.CollectionResponse> getDetail(@PathVariable Long id) {
        return Result.ok(collectionService.getCollectionRecord(id));
    }
}