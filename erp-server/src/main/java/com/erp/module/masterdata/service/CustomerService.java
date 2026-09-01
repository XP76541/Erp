package com.erp.module.masterdata.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.PageResult;
import com.erp.module.masterdata.entity.Customer;
import com.erp.module.masterdata.mapper.CustomerMapper;
import com.erp.module.system.service.DocSequenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final DocSequenceService docSequenceService;

    public PageResult<Customer> page(long page, long size, String keyword) {
        Page<Customer> result = customerMapper.selectPage(new Page<>(page, size),
                Wrappers.<Customer>lambdaQuery()
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(Customer::getName, keyword)
                                        .or().like(Customer::getCode, keyword)
                                        .or().like(Customer::getShortName, keyword)
                                        .or().like(Customer::getContact, keyword)
                                        .or().like(Customer::getPhone, keyword))
                        .orderByDesc(Customer::getId));
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Transactional
    public Long create(Customer customer) {
        if (!StringUtils.hasText(customer.getCode())) {
            // 客户编码自动生成:CUS + 6 位序号
            customer.setCode("CUS" + String.format("%06d", docSequenceService.next("CUS", "ALL")));
        }
        checkDuplicate(customer, null);
        customer.setId(null);
        if (customer.getSalespersonId() == null) {
            customer.setSalespersonId(0L);
        }
        customer.setIsActive(1);
        customerMapper.insert(customer);
        return customer.getId();
    }

    @Transactional
    public void update(Customer customer) {
        Customer db = requireById(customer.getId());
        if (db.getIsActive() == 0) {
            throw new BusinessException("已停用档案不能修改,请先启用");
        }
        checkDuplicate(customer, customer.getId());
        // 编码生成后不变,状态走独立接口
        customer.setCode(null);
        customer.setIsActive(null);
        customerMapper.updateById(customer);
    }

    /** 停用/启用(档案不物理删除);停用不影响历史单据与应收 */
    @Transactional
    public void toggleStatus(Long id, boolean active) {
        requireById(id);
        Customer p = new Customer();
        p.setId(id);
        p.setIsActive(active ? 1 : 0);
        customerMapper.updateById(p);
    }

    private Customer requireById(Long id) {
        Customer db = customerMapper.selectById(id);
        if (db == null) {
            throw new BusinessException("客户不存在");
        }
        return db;
    }

    /** 编码与名称全库唯一 */
    private void checkDuplicate(Customer customer, Long excludeId) {
        Long codeCount = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getCode, customer.getCode())
                .ne(excludeId != null, Customer::getId, excludeId));
        if (codeCount != null && codeCount > 0) {
            throw new BusinessException("客户编码已存在:" + customer.getCode());
        }
        Long nameCount = customerMapper.selectCount(Wrappers.<Customer>lambdaQuery()
                .eq(Customer::getName, customer.getName())
                .ne(excludeId != null, Customer::getId, excludeId));
        if (nameCount != null && nameCount > 0) {
            throw new BusinessException("客户名称已存在:" + customer.getName());
        }
    }
}
