package com.github.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtil {

    /** 元转换为分 */
    public static Long yuan2Cent(String yuan, int scale) {
        if (U.isNull(yuan)) {
            return null;
        }

        // 如果传 1_234_567 将转成 1234567 这样的整数
        if (yuan.contains("_")) {
            yuan = yuan.replace("_", "");
        }
        // 如果传 1,234,567 将转成 1234567 这样的整数
        if (yuan.contains(",")) {
            yuan = yuan.replace(",", "");
        }
        if (U.isNotNumber(yuan)) {
            U.assertException(String.format("金额(%s)必须是数字", yuan));
        }
        return new BigDecimal(yuan).movePointRight(scale).longValue();
    }
    /** 分转换为元 */
    public static String cent2Yuan(Long cent, int scale) {
        if (U.isNull(cent)) {
            return U.EMPTY;
        }
        if (cent == 0) {
            return "0";
        }
        String yuan = (cent > 0 ? U.EMPTY : "-") + BigDecimal.valueOf(Math.abs(cent)).movePointLeft(scale);
        if (yuan.endsWith("0")) {
            yuan = yuan.substring(0, yuan.length() - 1);
            if (yuan.endsWith("0")) {
                yuan = yuan.substring(0, yuan.length() - 2);
            }
        }
        return yuan;
    }
    /** 分转换为带千分位的元 */
    public static String cent2ShowYuan(Long cent) {
        return U.formatNumberToThousands(cent2Yuan(cent, 2));
    }


    // 上面主要是用 long, 下面是 BigDecimal, 前者比后者的运算效率要好一些

    /** 设置金额的精度, 忽略后面的精度 */
    public static BigDecimal setPrecision(BigDecimal money, int scale) {
        return U.isNull(money) ? null : money.setScale(scale, RoundingMode.DOWN);
    }

    /** num1 + num2 */
    public static BigDecimal add(BigDecimal num1, BigDecimal num2) {
        BigDecimal n1 = U.isNull(num1) ? BigDecimal.ZERO : num1;
        BigDecimal n2 = U.isNull(num2) ? BigDecimal.ZERO : num2;
        return n1.add(n2);
    }
    /** num1 - num2 */
    public static BigDecimal subtract(BigDecimal num1, BigDecimal num2) {
        BigDecimal n1 = U.isNull(num1) ? BigDecimal.ZERO : num1;
        BigDecimal n2 = U.isNull(num2) ? BigDecimal.ZERO : num2;
        return n1.subtract(n2);
    }
    /** num1 * num2 */
    public static BigDecimal multiply(BigDecimal num1, int num2) {
        return multiply(num1, new BigDecimal(num2));
    }
    /** num1 * num2 */
    public static BigDecimal multiply(BigDecimal num1, BigDecimal num2) {
        if (U.isNull(num2)) {
            return num1;
        } else if (U.isNull(num1)) {
            return num2;
        } else {
            return num1.multiply(num2);
        }
    }
    /** num1 / num2 返回有 2 位小数点精度的结果, 忽略 2 位后的小数位 */
    public static BigDecimal divide(BigDecimal num1, int num2) {
        return divideScale(num1, new BigDecimal(num2), 2);
    }
    /** num1 / num2 返回指定小数点精度的结果, 忽略之后小数位 */
    public static BigDecimal divideScale(BigDecimal num1, BigDecimal num2, int scale) {
        return divideScaleMode(num1, num2, scale, RoundingMode.DOWN);
    }
    /** num1 / num2 返回有指定位小数点精度的结果, 指定精度取舍 */
    public static BigDecimal divideScaleMode(BigDecimal num1, BigDecimal num2, int scale, RoundingMode mode) {
        U.assertException(num2 == null || num2.doubleValue() == 0, "除数要有值且不能为 0");

        BigDecimal n1 = U.isNull(num1) ? BigDecimal.ZERO : num1;
        return n1.divide(num2, scale, mode);
    }
}
