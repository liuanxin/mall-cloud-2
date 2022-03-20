package com.github.common.util;

import java.util.regex.Pattern;

/** 转换中文大写 */
public final class ChineseConvert {

    /** 整数和小数位之间的分隔 */
    private static final String SPLIT = " ";
    private static final String WHOLE = "整";
    private static final String NEGATIVE = "负";

    private static final String[] INTEGER = {
            "圆", "拾", "佰", "仟",
            "万", "拾", "佰", "仟",
            "亿", "拾", "佰", "仟"
    };
    private static final String[] DECIMAL = {"角", "分", "厘", "毫", "丝", "忽", "微"};
    private static final String[] NUM = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};

    private static final String MONEY_NOT_EFFECTIVE = "不是有效的金额";
    private static final String MAX_CONVERT = "最大只能转换到小数位前 " + INTEGER.length + " 位";
    private static final String MIN_CONVERT = "最小只能转换到小数位后 " + DECIMAL.length + " 位(" + DECIMAL[0] + ")";

    private static final Pattern ZERO_THOUSAND = Pattern.compile("零仟");
    private static final Pattern ZERO_HUNDRED = Pattern.compile("零佰");
    private static final Pattern ZERO_TEN = Pattern.compile("零拾");

    private static final Pattern THREE_ZERO = Pattern.compile("零零零");
    private static final Pattern TWO_ZERO = Pattern.compile("零零");

    private static final Pattern ZERO_BILLION = Pattern.compile("零亿");
    private static final Pattern ZERO_MILLION = Pattern.compile("零万");
    private static final Pattern BILLION_MILLION = Pattern.compile("亿万");

    private static final Pattern ONE_TEN = Pattern.compile("壹拾");
    private static final Pattern ZERO_DOLLAR = Pattern.compile("零圆");

    private static final Pattern ZERO_DIME = Pattern.compile("零角");
    private static final Pattern ZERO_CENT = Pattern.compile("零分");

    private static final String ZERO = "零";
    private static final String BILLION = "亿";
    private static final String MILLION = "万";
    private static final String TEN = "拾";
    private static final String DOLLAR = "圆";
    private static final String BLANK = "";

    /**
     * 转换大写
     *
     * @param money 金额
     * @return 大写
     */
    public static String upperCase(String money) {
        if (money == null || money.trim().length() == 0) {
            return BLANK;
        }
        // 如果是 0 直接返回
        try {
            if (Double.parseDouble(money) == 0) {
                return NUM[0] + INTEGER[0] + WHOLE;
            }
        } catch (NumberFormatException nfe) {
            return MONEY_NOT_EFFECTIVE;
        }

        // 基本的位数检查, 按小数拆分, 分别处理
        int pointIndex = money.indexOf(".");
        boolean hasPoint = pointIndex > -1;

        String left = hasPoint ? money.substring(0, pointIndex) : money;
        long leftLong = U.toLong(left);
        boolean negative = leftLong < 0;
        if (negative) {
            left = left.substring(1);
        }
        int leftLen = left.length();
        if (leftLen > INTEGER.length) {
            return MAX_CONVERT;
        }

        // 处理小数位后面的值
        String right = hasPoint ? money.substring(pointIndex + 1) : "";
        int rightLen = right.length();
        if (rightLen > DECIMAL.length) {
            // right = right.substring(0, DECIMAL.length);
            return MIN_CONVERT;
        }

        StringBuilder sbd = new StringBuilder();
        // 处理小数位前面的数
        if (leftLong != 0) {
            if (negative) {
                sbd.append(NEGATIVE);
            }
            for (int i = 0; i < leftLen; i++) {
                int number = U.toInt(String.valueOf(left.charAt(i)));
                sbd.append(NUM[number]).append(INTEGER[leftLen - i - 1]);
            }
        }
        // 处理小数位后面的值
        long rightLong = U.toLong(right);
        if (rightLong > 0) {
            sbd.append(SPLIT);
            for (int i = 0; i < rightLen; i++) {
                int number = U.toInt(String.valueOf(right.charAt(i)));
                sbd.append(NUM[number]).append(DECIMAL[i]);
            }
        } else if (rightLong == 0) {
            sbd.append(WHOLE);
        }

        // 最后的收尾工作, 替换成可读性更强的
        /*
        return sbd.toString().replaceAll("零仟", ZERO).replaceAll("零佰", ZERO).replaceAll("零拾",ZERO)
                .replaceAll("零零零", ZERO).replaceAll("零零", ZERO)
                .replaceAll("零亿", BILLION).replaceAll("零万", MILLION).replaceAll("亿万", BILLION)
                .replaceAll("壹拾", TEN).replaceAll("零圆", DOLLAR)
                .replaceAll("零角", BLANK).replaceAll("零分", BLANK);
        */
        String result = sbd.toString();
        result = ZERO_THOUSAND.matcher(result).replaceAll(ZERO);
        result = ZERO_HUNDRED.matcher(result).replaceAll(ZERO);
        result = ZERO_TEN.matcher(result).replaceAll(ZERO);

        result = THREE_ZERO.matcher(result).replaceAll(ZERO);
        result = TWO_ZERO.matcher(result).replaceAll(ZERO);

        result = ZERO_BILLION.matcher(result).replaceAll(BILLION);
        result = ZERO_MILLION.matcher(result).replaceAll(MILLION);
        result = BILLION_MILLION.matcher(result).replaceAll(BILLION);

        result = ONE_TEN.matcher(result).replaceAll(TEN);
        result = ZERO_DOLLAR.matcher(result).replaceAll(DOLLAR);

        result = ZERO_DIME.matcher(result).replaceAll(BLANK);
        result = ZERO_CENT.matcher(result).replaceAll(BLANK);
        return result;
    }
}
