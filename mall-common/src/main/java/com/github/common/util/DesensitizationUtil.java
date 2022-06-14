package com.github.common.util;

import com.github.common.Const;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 脱敏工具类 */
public final class DesensitizationUtil {

    private static final List<String> SENSITIVE_LIST = Arrays.asList("*", "**", "****", "*****");

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    /** 基于 key 脱敏 */
    public static String desKey(String key, String value) {
        if (U.isBlank(key) || U.isBlank(value)) {
            return SENSITIVE_LIST.get(U.RANDOM.nextInt(SENSITIVE_LIST.size()));
        }

        String lower = key.toLowerCase();
        if (lower.equals(Const.TOKEN.toLowerCase())) {
            return U.foggyToken(value);
        }

        switch (lower) {
            case "password": {
                return "***";
            }
            case "phone": {
                return U.foggyPhone(value);
            }
            case "id-card":
            case "idcard":
            case "id_card": {
                return U.foggyIdCard(value);
            }
            default: {
                return U.foggyValue(value, 300, 100);
            }
        }
    }

    /** 字符串脱敏, 将开始索引和结束索引之间的字符替换成「***」 */
    public static String desString(String value, int startIndex, int endIndex) {
        int start = Math.max(0, startIndex);
        int end = Math.max(0, endIndex);
        int length = value.length();

        StringBuilder sbd = new StringBuilder();
        if (start < length) {
            sbd.append(value, 0, start).append(" ");
        }
        sbd.append("***");
        if (end > 0 && length > (start + end + 3)) {
            sbd.append(" ").append(value, length - end, length);
        }
        return sbd.toString().trim();
    }

    /** 数字脱敏, 浮点数随机且指定小数位并输出成字符串, 大数随机并输出成字符串, 小数随机 */
    public static Object descNumber(Number value, double randomNumber, int digitsNumber) {
        double random = Math.abs(randomNumber);
        if (value instanceof BigDecimal || value instanceof Double || value instanceof Float) {
            double d = (random != 0) ? RANDOM.nextDouble(random) : value.doubleValue();
            // BigDecimal 或 float 或 double 使用 String 序列化
            return BigDecimal.valueOf(d).setScale(Math.abs(digitsNumber), RoundingMode.DOWN).toString();
        }
        if (value instanceof BigInteger || value instanceof Long) {
            // long 或 BigInt 使用 String 序列化
            return Long.toString((random != 0) ?  RANDOM.nextLong((long) random) : value.longValue());
        }
        if (value instanceof Integer || value instanceof Short) {
            return (random != 0) ? RANDOM.nextInt((int) random) : value.intValue();
        }
        return value;
    }

    public static void descDate(Date value, long randomDateTimeMillis) {
        long random = Math.abs(randomDateTimeMillis);
        if (random != 0) {
            value.setTime(Math.abs(value.getTime() - Math.max(1L, RANDOM.nextLong(random))));
        }
    }
}
