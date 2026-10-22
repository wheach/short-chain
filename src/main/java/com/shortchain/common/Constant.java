package com.shortchain.common;

public class Constant {
    /** 长链最大长度 */
    public static final int MAX_URL_LENGTH = 2048;

    /** 短链缓存 key 前缀：short:code:{shortCode} -> 原始长链 */
    public static final String SHORT_CODE = "short:code:";

    /** 短链缓存过期时间（秒），防止 Redis 内存无限膨胀 */
    public static final long SHORT_CODE_TTL = 7 * 24 * 60 * 60L; // 7 天

    /** 幂等 key（sm3 -> shortCode）的过期时间（秒） */
    public static final long SM3_CODE_TTL = 7 * 24 * 60 * 60L; // 7 天

    public static final long SM3_SETNX_TTL = 30;
}
