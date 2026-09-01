package com.erp.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页响应结构
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private long total;
    private List<T> records;

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, records);
    }
}
