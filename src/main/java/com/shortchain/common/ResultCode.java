package com.shortchain.common;

import lombok.Getter;

/**
 * 业务错误码枚举
 * 与接口文档 1.3 错误码表对齐
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "success", 200),

    /** 参数错误 */
    PARAM_ERROR(40001, "参数错误", 400),

    /** 长链不合法（非 http/https 或格式错误） */
    INVALID_URL(40002, "原始链接不合法，仅支持 http/https", 400),

    /** 长链长度超限 */
    URL_TOO_LONG(40003, "链接长度不能超过 2048", 400),

    /** 短码不存在 */
    NOT_FOUND(40401, "短链不存在", 404),

    /** 请求过于频繁（Sentinel 整体限流） */
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试", 429),

    /** 单个短链访问过于频繁（Sentinel 热点参数限流） */
    HOT_PARAM_LIMITED(42901, "该短链访问过于频繁，请稍后再试", 429),

    /** 服务暂时不可用（Sentinel 熔断降级） */
    SERVICE_DEGRADED(50301, "服务暂时不可用，请稍后再试", 503),

    /** 系统内部错误 */
    SYSTEM_ERROR(50000, "系统内部错误", 500);

    /** 业务状态码，0 表示成功，非 0 表示失败 */
    private final int code;

    /** 提示信息 */
    private final String message;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    ResultCode(int code, String message,int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
