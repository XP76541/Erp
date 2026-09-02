package com.erp.module.finance.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.finance.dto.ReceivableDtos;
import com.erp.module.finance.entity.CollectionRecord;
import com.erp.module.finance.entity.Receivable;
import com.erp.module.finance.mapper.CollectionRecordMapper;
import com.erp.module.finance.mapper.ReceivableMapper;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.TokenStore;
import com.erp.module.system.service.OperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 催收服务
 */
@Service
public class CollectionService {

    private final CollectionRecordMapper collectionRecordMapper;
    private final ReceivableMapper receivableMapper;
    private final CustomerMapper customerMapper;
    private final OperationLogService operationLogService;

    public CollectionService(CollectionRecordMapper collectionRecordMapper,
                           ReceivableMapper receivableMapper,
                           CustomerMapper customerMapper,
                           OperationLogService operationLogService) {
        this.collectionRecordMapper = collectionRecordMapper;
        this.receivableMapper = receivableMapper;
        this.customerMapper = customerMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 创建催收记录
     */
    @Transactional
    public Long createCollectionRecord(ReceivableDtos.CollectionRequest request, TokenStore.LoginUser user) {
        // 获取应收账款信息
        Receivable receivable = receivableMapper.selectById(request.getReceivableId());
        if (receivable == null) {
            throw new BusinessException("应收账款记录不存在");
        }

        if (!"UNSETTLED".equals(receivable.getStatus()) && !"PARTIAL".equals(receivable.getStatus())) {
            throw new BusinessException("只能对未结算或部分结算的应收账款进行催收");
        }

        // 创建催收记录
        CollectionRecord record = new CollectionRecord();
        record.setReceivableId(receivable.getId());
        record.setReceivableDocNo(receivable.getDocNo());
        record.setCustomerId(receivable.getCustomerId());
        record.setCustomerName(receivable.getCustomerName());
        record.setAmount(receivable.getRemainingAmount());
        record.setContactMethod(request.getContactMethod());
        record.setContactTime(LocalDateTime.now().toString());
        record.setContactResult(request.getContactResult());
        record.setNextAction(request.getNextAction());
        record.setOperator(String.valueOf(user.userId()));
        record.setOperatorName(user.realName());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        collectionRecordMapper.insert(record);

        // 记录操作日志
        operationLogService.record(user, "collection", "CREATE",
                "RECEIVABLE", receivable.getId(), receivable.getDocNo(),
                "{\"contactMethod\":\"" + request.getContactMethod() +
                 "\",\"contactResult\":\"" + request.getContactResult() +
                 "\",\"nextAction\":\"" + request.getNextAction() + "\"}",
                request.getRemark());

        return record.getId();
    }

    /**
     * 获取应收账款的催收记录
     */
    public List<ReceivableDtos.CollectionResponse> getCollectionRecordsByReceivable(Long receivableId) {
        List<CollectionRecord> records = collectionRecordMapper.getByReceivableId(receivableId);
        return records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取客户的催收记录
     */
    public PageResult<ReceivableDtos.CollectionResponse> getCollectionRecordsByCustomer(Long customerId,
                                                                                    int page,
                                                                                    int size) {
        List<CollectionRecord> records = collectionRecordMapper.getByCustomerId(customerId);
        List<ReceivableDtos.CollectionResponse> responses = records.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // 简单分页实现
        int start = (page - 1) * size;
        int end = Math.min(start + size, responses.size());
        List<ReceivableDtos.CollectionResponse> pageRecords = responses.subList(start, end);

        return PageResult.of(records.size(), pageRecords);
    }

    /**
     * 获取催收记录详情
     */
    public ReceivableDtos.CollectionResponse getCollectionRecord(Long id) {
        CollectionRecord record = collectionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("催收记录不存在");
        }
        return convertToResponse(record);
    }

    /**
     * 获取催收统计
     */
    public ReceivableDtos.CollectionStatsResponse getCollectionStatistics() {
        List<CollectionRecordMapper.CollectionStatistics> statistics = collectionRecordMapper.getCollectionStatistics();

        ReceivableDtos.CollectionStatsResponse response = new ReceivableDtos.CollectionStatsResponse();
        response.setTotalOverdueCount(getOverdueCount());
        response.setTotalOverdueAmount(getOverdueAmount());
        response.setStatusCounts(calculateStatusCounts(statistics));

        return response;
    }

    /**
     * 自动生成催收计划
     */
    public List<Receivable> generateCollectionPlan() {
        // 获取逾期超过30天且最近30天内未催收的应收账款
        List<Receivable> overdueReceivables = receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .gt(Receivable::getDaysOverdue, 30)
                        .eq(Receivable::getStatus, "UNSETTLED")
                        .le(Receivable::getRemainingAmount, new BigDecimal("0.01"))
        );

        // 过滤掉最近30天内有催收记录的单据
        return overdueReceivables.stream()
                .filter(receivable -> {
                    List<CollectionRecord> recentRecords = collectionRecordMapper.getByReceivableId(receivable.getId());
                    return recentRecords.stream()
                            .noneMatch(record -> {
                                LocalDateTime recordTime = record.getCreatedAt();
                                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                                return recordTime.isAfter(thirtyDaysAgo);
                            });
                })
                .collect(Collectors.toList());
    }

    /**
     * 更新催收记录
     */
    @Transactional
    public void updateCollectionRecord(Long id, ReceivableDtos.CollectionRequest request, TokenStore.LoginUser user) {
        CollectionRecord record = collectionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("催收记录不存在");
        }

        record.setContactResult(request.getContactResult());
        record.setNextAction(request.getNextAction());
        record.setUpdatedAt(LocalDateTime.now());
        collectionRecordMapper.updateById(record);

        // 记录操作日志
        operationLogService.record(user, "collection", "UPDATE",
                "COLLECTION", record.getId(), null,
                "{\"contactResult\":\"" + request.getContactResult() +
                 "\",\"nextAction\":\"" + request.getNextAction() + "\"}",
                request.getRemark());
    }

    /**
     * 获取需要升级处理的催收记录
     */
    public List<CollectionRecord> getRecordsForEscalation() {
        // 获取标记为"催收困难"超过15天的记录
        LocalDate fifteenDaysAgo = LocalDate.now().minusDays(15);
        return collectionRecordMapper.getFailedCollectionRecords("NEED_FOLLOW_UP", fifteenDaysAgo);
    }

    /**
     * 批量催收
     */
    @Transactional
    public void batchCollection(List<ReceivableDtos.CollectionRequest> requests, TokenStore.LoginUser user) {
        for (ReceivableDtos.CollectionRequest request : requests) {
            createCollectionRecord(request, user);
        }
    }

    // 私有辅助方法
    private ReceivableDtos.CollectionResponse convertToResponse(CollectionRecord record) {
        ReceivableDtos.CollectionResponse response = new ReceivableDtos.CollectionResponse();
        response.setId(record.getId());
        response.setReceivableDocNo(record.getReceivableDocNo());
        response.setCustomerId(record.getCustomerId());
        response.setCustomerName(record.getCustomerName());
        response.setAmount(record.getAmount());
        // 逾期天数从receivable获取
        Receivable receivable = receivableMapper.selectById(record.getReceivableId());
        response.setDaysOverdue(receivable != null ? receivable.getDaysOverdue() : null);
        response.setContactMethod(record.getContactMethod());
        response.setContactResult(record.getContactResult());
        response.setNextAction(record.getNextAction());
        response.setOperator(record.getOperatorName());
        response.setCreatedAt(record.getCreatedAt() != null ? record.getCreatedAt().toString() : null);
        return response;
    }

    private Integer getOverdueCount() {
        return receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .gt(Receivable::getDaysOverdue, 0)
                        .eq(Receivable::getStatus, "UNSETTLED")
        ).size();
    }

    private BigDecimal getOverdueAmount() {
        return receivableMapper.selectList(
                Wrappers.<Receivable>lambdaQuery()
                        .gt(Receivable::getDaysOverdue, 0)
                        .eq(Receivable::getStatus, "UNSETTLED")
        ).stream()
                .map(Receivable::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<ReceivableDtos.CollectionStatsResponse.CollectionStatusCount> calculateStatusCounts(
            List<CollectionRecordMapper.CollectionStatistics> statistics) {
        return statistics.stream()
                .map(stat -> {
                        ReceivableDtos.CollectionStatsResponse.CollectionStatusCount count = new ReceivableDtos.CollectionStatsResponse.CollectionStatusCount();
                        count.setStatus(stat.getContactResult());
                        count.setCount(stat.getCount().intValue());
                        count.setAmount(stat.getAmount());
                        return count;
                })
                .collect(Collectors.toList());
    }
}