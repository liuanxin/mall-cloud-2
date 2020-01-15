package com.github.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * 运算全部基于到分的 long 来做, 它的性能比 BigDecimal 要好很多
 * 链式运算时, 如 Money count = xxx.plus(y).lessSelf(z).multiplySelf(n).divideSelf(m) 这种
 * 第一次不要用 self, 后面的都加上 self, 这样只会实例化一次.
 * 如果都不带 self 则每一次运算都将实例化一个 Money 对象
 * 如果都带 self 将会改变第一个调用的值, 如上面的示例如果每次调用都带 self 将会改变 xxx 的值
 */
public class Money implements Serializable {
    private static final long serialVersionUID = 0L;

    /** 元分换算的比例. 左移右移的位数 */
    private static final int SCALE = 2;

    /** 实际存储进数据库的值 */
    private Long cent;

    public Money() {}
    /** 从数据库过来的数据, 使用此构造 */
    public Money(Long cent) {
        this.cent = cent;
    }
    /** 从前台过来的数据转换成金额对象时使用此构造 */
    @JsonCreator
    public Money(String yuan) {
        cent = yuan2Cent(yuan);
    }

    /** 在前台或者在页面上显示 */
    @JsonValue
    @Override
    public String toString() {
        return cent2Yuan(cent);
    }

    /** 输出 1,234,567,890.50 的形式 */
    public String toShow() {
        return cent2ShowYuan(cent);
    }

    public Long getCent() {
        return cent;
    }
    public void setCent(Long cent) {
        this.cent = cent;
    }


    public Money plus(Money money) {
        return new Money(cent + money.cent);
    }
    public Money plusSelf(Money money) {
        cent += money.cent;
        return this;
    }

    public Money less(Money money) {
        return new Money(cent - money.cent);
    }
    public Money lessSelf(Money money) {
        cent -= money.cent;
        return this;
    }

    public Money multiply(int num) {
        return new Money(cent * num);
    }
    public Money multiplySelf(int num) {
        cent *= num;
        return this;
    }

    public Money divide(int num) {
        return new Money(cent / num);
    }
    public Money divideSelf(int num) {
        cent /= num;
        return this;
    }


    /** 检查金额是否是负数 */
    public void checkEmptyAndNegative() {
        if (cent == null || cent < 0) {
            U.assertException("金额不能是负数");
        }
    }

    /** 输出大写中文 */
    public String toChinese() {
        return ChineseConvert.upperCase(toString());
    }


    /** 元转换为分 */
    public static Long yuan2Cent(String yuan) {
        if (U.isBlank(yuan)) {
            return null;
        }
        if (yuan.contains("_")) {
            yuan = yuan.replace("_", "");
        }
        if (yuan.contains(",")) {
            yuan = yuan.replace(",", "");
        }
        if (U.isNotNumber(yuan)) {
            U.assertException(String.format("金额(%s)必须是数字", yuan));
        }
        return new BigDecimal(yuan).movePointRight(SCALE).longValue();
    }
    /** 分转换为元 */
    public static String cent2Yuan(Long cent) {
        if (U.isBlank(cent)) {
            return U.EMPTY;
        }
        if (cent == 0) {
            return "0";
        }
        return (cent > 0 ? U.EMPTY : "-") + BigDecimal.valueOf(Math.abs(cent)).movePointLeft(SCALE).toString();
    }
    /** 分转换为带千分位的元 */
    public static String cent2ShowYuan(Long cent) {
        return U.formatNumberToThousands(cent2Yuan(cent));
    }


    // 上面主要是用 long, 下面是 BigDecimal, 前者比后者的运算效率要好一些

    /** 设置金额的精度 */
    public static BigDecimal setPrecision(BigDecimal money) {
        return U.isBlank(money) ? money : money.setScale(SCALE, RoundingMode.DOWN);
    }

    /** num1 + num2 */
    public static BigDecimal add(BigDecimal num1, BigDecimal num2) {
        BigDecimal n1 = U.isBlank(num1) ? BigDecimal.ZERO : num1;
        BigDecimal n2 = U.isBlank(num2) ? BigDecimal.ZERO : num2;
        return n1.add(n2);
    }
    /** num1 - num2 */
    public static BigDecimal subtract(BigDecimal num1, BigDecimal num2) {
        BigDecimal n1 = U.isBlank(num1) ? BigDecimal.ZERO : num1;
        BigDecimal n2 = U.isBlank(num2) ? BigDecimal.ZERO : num2;
        return n1.subtract(n2);
    }
    /** num1 * num2 */
    public static BigDecimal multiply(BigDecimal num1, int num2) {
        return multiply(num1, new BigDecimal(num2));
    }
    /** num1 * num2 第二个参数为空则是 1 */
    public static BigDecimal multiply(BigDecimal num1, BigDecimal num2) {
        if (U.isBlank(num2)) {
            return num1;
        } else {
            BigDecimal n1 = U.isBlank(num1) ? BigDecimal.ZERO : num1;
            return n1.multiply(num2);
        }
    }
    /** num1 / num2 返回有 2 位小数点精度的结果 */
    public static BigDecimal divide(BigDecimal num1, Integer num2) {
        return divide(num1, new BigDecimal(num2));
    }
    /** num1 / num2 返回有 2 位小数点精度的结果 */
    public static BigDecimal divide(BigDecimal num1, BigDecimal num2) {
        U.assertException(num2 == null || num2.doubleValue() == 0, "除数要有值且不能为 0");

        BigDecimal n1 = U.isBlank(num1) ? BigDecimal.ZERO : num1;
        return n1.divide(num2, SCALE, RoundingMode.DOWN);
    }


    /** 转换中文大写 */
    private static final class ChineseConvert {
        /** 整数和小数位之间的分隔 */
        private static final String SPLIT = " ";
        private static final String WHOLE = "整";
        private static final String NEGATIVE = "负";

        private static final String[] INTEGER = {
                "圆", "拾", "佰", "仟",
                "万", "拾", "佰", "仟",
                "亿", "拾", "佰", "仟"
        };
        private static final String[] DECIMAL = {/*"厘", */"分", "角"};
        private static final String[] NUM = {
                "零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"
        };

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
        private static String upperCase(String money) {
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
            String left = money.contains(".") ? money.substring(0, money.indexOf(".")) : money;
            long leftLong = U.toLong(left);
            if (leftLong < 0) {
                left = left.substring(1);
            }
            if (left.length() > INTEGER.length) {
                return MAX_CONVERT;
            }
            String right = money.contains(".") ? money.substring(money.indexOf(".") + 1) : "";
            if (right.length() > DECIMAL.length) {
                // right = right.substring(0, DECIMAL.length);
                return MIN_CONVERT;
            }

            StringBuilder sbd = new StringBuilder();
            // 处理小数位前面的数
            if (leftLong != 0) {
                if (leftLong < 0) {
                    sbd.append(NEGATIVE);
                }
                for (int i = 0; i < left.length(); i++) {
                    int number = U.toInt(String.valueOf(left.charAt(i)));
                    sbd.append(NUM[number]).append(INTEGER[left.length() - i - 1]);
                }
            }

            // 处理小数位后面的值
            long rightLong = U.toLong(right);
            if (rightLong > 0) {
                sbd.append(SPLIT);
                for (int i = 0; i < right.length(); i++) {
                    int number = U.toInt(String.valueOf(right.charAt(i)));
                    sbd.append(NUM[number]).append(DECIMAL[right.length() - i - 1]);
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
}
