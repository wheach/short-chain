package com.shortchain.entity;

import com.shortchain.common.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体
 * 结构：{ code, message, data }
 * 与接口文档 1.2 统一响应结构对齐
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    /** 业务状态码，0 表示成功，非 0 表示失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据，失败时为 null */
    private Object data;

    /** 成功（无数据） */
    public static Result ok() {
        return new Result(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功（携带数据） */
    public static Result ok(Object data) {
        return new Result(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 失败（指定错误码） */
    public static Result fail(ResultCode resultCode) {
        return new Result(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败（自定义提示信息，默认系统错误码） */
    public static Result fail(String message) {
        return new Result(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }
}
