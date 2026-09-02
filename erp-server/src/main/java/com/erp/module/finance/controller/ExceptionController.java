package com.erp.module.finance.controller;

import com.erp.common.Result;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.ReceivableException;
import com.erp.module.finance.service.ExceptionMonitorService;
import com.erp.module.system.TokenStore;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 应收账款异常管理Controller
 */
@RestController
@RequestMapping("/finance/exceptions")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionMonitorService exceptionService;

    /**
     * 手动创建异常
     */
    @PostMapping
    public Result<Long> createException(@RequestBody ReceivableDtos.ExceptionRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        Long id = exceptionService.createManualException(request, currentUser);
        return Result.ok(id);
    }

    /**
     * 获取所有待处理的异常
     */
    @GetMapping("/open")
    public Result<List<ReceivableDtos.CollectionResponse>> getOpenExceptions() {
        // 通过服务方法获取异常列表
        List<com.erp.module.finance.entity.ReceivableException> exceptions = exceptionService.getOpenExceptions();

        return Result.ok(exceptions.stream()
                .map(exception -> {
                    ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
                    response.setId(exception.getId());
                    response.setReceivableDocNo(exception.getReceivableDocNo());
                    response.setCustomerId(exception.getCustomerId());
                    response.setCustomerName(exception.getCustomerName());
                    response.setAmount(null); // 异常不涉及金额
                    response.setDaysOverdue(null);
                    response.setContactMethod(null);
                    response.setContactResult(null);
                    response.setNextAction(null);
                    response.setOperator(exception.getCreatedBy());
                    response.setCreatedAt(exception.getCreatedAt().toString());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 获取高优先级异常
     */
    @GetMapping("/high-priority")
    public Result<List<ReceivableDtos.CollectionResponse>> getHighPriorityExceptions() {
        List<com.erp.module.finance.entity.ReceivableException> exceptions = exceptionService.getHighPriorityExceptions();

        return Result.ok(exceptions.stream()
                .map(exception -> {
                    ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
                    response.setId(exception.getId());
                    response.setReceivableDocNo(exception.getReceivableDocNo());
                    response.setCustomerId(exception.getCustomerId());
                    response.setCustomerName(exception.getCustomerName());
                    response.setAmount(null);
                    response.setDaysOverdue(null);
                    response.setContactMethod(null);
                    response.setContactResult(null);
                    response.setNextAction(null);
                    response.setOperator(exception.getCreatedBy());
                    response.setCreatedAt(exception.getCreatedAt().toString());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 获取指定客户的异常
     */
    @GetMapping("/customer/{customerId}")
    public Result<List<ReceivableDtos.CollectionResponse>> getExceptionsByCustomer(@PathVariable Long customerId) {
        List<com.erp.module.finance.entity.ReceivableException> exceptions = exceptionService.getExceptionsByCustomer(customerId);

        return Result.ok(exceptions.stream()
                .map(exception -> {
                    ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
                    response.setId(exception.getId());
                    response.setReceivableDocNo(exception.getReceivableDocNo());
                    response.setCustomerId(exception.getCustomerId());
                    response.setCustomerName(exception.getCustomerName());
                    response.setAmount(null);
                    response.setDaysOverdue(null);
                    response.setContactMethod(null);
                    response.setContactResult(null);
                    response.setNextAction(null);
                    response.setOperator(exception.getCreatedBy());
                    response.setCreatedAt(exception.getCreatedAt().toString());
                    return response;
                })
                .collect(java.util.stream.Collectors.toList()));
    }

    /**
     * 解决异常
     */
    @PutMapping("/{id}/resolve")
    public Result<Void> resolveException(@PathVariable Long id, @RequestBody ReceivableDtos.ExceptionResolutionRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        exceptionService.resolveException(id, request, currentUser);
        return Result.ok();
    }

    /**
     * 忽略异常
     */
    @PutMapping("/{id}/ignore")
    public Result<Void> ignoreException(@PathVariable Long id, @RequestParam String reason) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();
        exceptionService.ignoreException(id, reason, currentUser);
        return Result.ok();
    }

    /**
     * 获取异常统计
     */
    @GetMapping("/statistics")
    public Result<ReceivableDtos.ExceptionStatisticsResponse> getExceptionStatistics() {
        ReceivableDtos.ExceptionStatisticsResponse statistics = exceptionService.getExceptionStatistics();
        return Result.ok(statistics);
    }

    /**
     * 获取异常趋势
     */
    @GetMapping("/trend")
    public Result<List<ReceivableDtos.ExceptionTrendResponse>> getExceptionTrend(@RequestParam(defaultValue = "30") Integer days) {
        List<ReceivableDtos.ExceptionTrendResponse> trends = exceptionService.getExceptionTrend(days);
        return Result.ok(trends);
    }

    /**
     * 获取异常详情
     */
    @GetMapping("/{id}")
    public Result<ExceptionDetail> getExceptionDetail(@PathVariable Long id) {
        com.erp.module.finance.entity.ReceivableException exception = exceptionService.getExceptionById(id);
        if (exception == null) {
            return Result.fail("异常记录不存在");
        }

        ExceptionDetail detail = new ExceptionDetail();
        detail.setId(exception.getId());
        detail.setReceivableId(exception.getReceivableId());
        detail.setReceivableDocNo(exception.getReceivableDocNo());
        detail.setCustomerId(exception.getCustomerId());
        detail.setCustomerName(exception.getCustomerName());
        detail.setExceptionType(exception.getExceptionType());
        detail.setExceptionLevel(exception.getExceptionLevel());
        detail.setDescription(exception.getDescription());
        detail.setImpact(exception.getImpact());
        detail.setSuggestedAction(exception.getSuggestedAction());
        detail.setStatus(exception.getStatus());
        detail.setAssignedTo(exception.getAssignedTo());
        detail.setCreatedBy(exception.getCreatedBy());
        detail.setCreatedByName(exception.getCreatedByName());
        detail.setCreatedAt(exception.getCreatedAt().toString());
        detail.setResolvedAt(exception.getResolvedAt() != null ? exception.getResolvedAt().toString() : null);
        detail.setResolution(exception.getResolution());

        return Result.ok(detail);
    }

    /**
     * 异常详情内部类
     */
    public static class ExceptionDetail {
        private Long id;
        private Long receivableId;
        private String receivableDocNo;
        private Long customerId;
        private String customerName;
        private String exceptionType;
        private String exceptionLevel;
        private String description;
        private String impact;
        private String suggestedAction;
        private String status;
        private String assignedTo;
        private String createdBy;
        private String createdByName;
        private String createdAt;
        private String resolvedAt;
        private String resolution;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getReceivableId() { return receivableId; }
        public void setReceivableId(Long receivableId) { this.receivableId = receivableId; }
        public String getReceivableDocNo() { return receivableDocNo; }
        public void setReceivableDocNo(String receivableDocNo) { this.receivableDocNo = receivableDocNo; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
        public String getExceptionLevel() { return exceptionLevel; }
        public void setExceptionLevel(String exceptionLevel) { this.exceptionLevel = exceptionLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }
        public String getSuggestedAction() { return suggestedAction; }
        public void setSuggestedAction(String suggestedAction) { this.suggestedAction = suggestedAction; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public String getCreatedByName() { return createdByName; }
        public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
    }

    /**
     * 批量处理异常
     */
    @PostMapping("/batch-process")
    public Result<Void> batchProcess(@RequestBody BatchProcessRequest request) {
        TokenStore.LoginUser currentUser = TokenStore.getCurrentLoginUser();

        for (Long id : request.getExceptionIds()) {
            if ("RESOLVE".equals(request.getAction())) {
                ReceivableDtos.ExceptionResolutionRequest resolutionRequest = new ReceivableDtos.ExceptionResolutionRequest();
                resolutionRequest.setResolution(request.getResolution());
                resolutionRequest.setAssignedTo(currentUser.realName());
                exceptionService.resolveException(id, resolutionRequest, currentUser);
            } else if ("IGNORE".equals(request.getAction())) {
                exceptionService.ignoreException(id, request.getReason(), currentUser);
            }
        }

        return Result.ok();
    }

    /**
     * 批量处理请求
     */
    public static class BatchProcessRequest {
        private List<Long> exceptionIds;
        private String action; // RESOLVE or IGNORE
        private String resolution;
        private String reason;

        // Getters and Setters
        public List<Long> getExceptionIds() { return exceptionIds; }
        public void setExceptionIds(List<Long> exceptionIds) { this.exceptionIds = exceptionIds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}