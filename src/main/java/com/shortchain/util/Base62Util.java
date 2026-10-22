package com.shortchain.util;

/**
 * 62 进制转换工具类
 * 字符集：0-9 a-z A-Z（共 62 个 URL 安全字符）
 * 用途：将雪花算法生成的 long 型 ID 转为短字符串短码
 */
public class Base62Util {

    /** 62 进制字符集 */
    private static final char[] BASE62_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /**
     * 将 long 型 ID 编码为 62 进制字符串
     *
     * @param id 待编码的 ID（雪花算法生成）
     * @return 62 进制短码
     */
    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(BASE62_CHARS[0]);
        }

        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            sb.append(BASE62_CHARS[(int) (value % 62)]);
            value /= 62;
        }
        // 反转，因为是从低位到高位拼接的
        return sb.reverse().toString();
    }

    /**
     * 将 62 进制字符串解码为 long（可选，反向映射用）
     *
     * @param base62 62 进制字符串
     * @return 解码后的 long
     */
    public static long decode(String base62) {
        long result = 0;
        for (int i = 0; i < base62.length(); i++) {
            char c = base62.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'z') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'Z') {
                digit = c - 'A' + 36;
            } else {
                throw new IllegalArgumentException("非法字符: " + c);
            }
            result = result * 62 + digit;
        }
        return result;
    }
}
