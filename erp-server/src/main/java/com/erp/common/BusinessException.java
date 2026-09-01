package com.erp.common;

import lombok.Getter;

/**
 * 业务异常:中断流程并向前端返回可读提示,如"库存不足""单据已审核不可修改"
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
