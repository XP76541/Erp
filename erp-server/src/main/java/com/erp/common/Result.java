package com.erp.common;

import lombok.Data;

/**
 * 统一响应结构:code = 0 成功,非 0 失败(401 未登录 / 403 无权限 / 400 参数错误 / 500 系统异常)
 */
@Data
public class Result<T> {

    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    /**
     * Compatibility alias for controllers using the historical method name.
     */
    public static <T> Result<T> success(T data) {
        return ok(data);
    }

    public static Result<Void> success() {
        return ok();
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}
