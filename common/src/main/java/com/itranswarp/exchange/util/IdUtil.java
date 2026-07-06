package com.itranswarp.exchange.util;

/**
 * 简单的 ID 工具。
 */
public final class IdUtil {

    /**
     * 校验字符串是否是一个合法的正整数 ID。
     */
    public static boolean isValidLongId(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            return Long.parseLong(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private IdUtil() {
    }
}
