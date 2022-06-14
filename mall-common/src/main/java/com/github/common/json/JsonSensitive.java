package com.github.common.json;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.*;

/**
 * <pre>
 * 脱敏注解. 只能标在 String、Number(Integer, Long, Float, Double, BigInteger, BigDecimal)、Date 类型上
 *
 * 用在 String:       &#064;JsonSensitive(start = 2, end = 2) 将 abcdefghijklmnopqrstuvwxyz 输出成 ab *** yz
 * 用在 Integer/Long: &#064;JsonSensitive(randomNumber = 100) 将输出成 1 ~ 100 之间的随机数
 * 用在 BigDecimal:   &#064;JsonSensitive(randomNumber = 123.534D, digitsNumber = 3) 将输出成 1 ~ 123.534 之间的随机数并保留 3 位小数
 * 用在 Date:         &#064;JsonSensitive(randomDate = 1234567890L) 将 2022-01-01 输出成 "2022-01-01 的时间戳 - (1 ~ 1234567890)" 的时间格式
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JsonSerialize(using = JsonSensitiveSerializer.class)
@JacksonAnnotationsInside
public @interface JsonSensitive {

    /** 用在字符串类型时要脱敏的首字符位, 比如字符串是「123456」, 当前值是 2, 则最后脱敏成「12 ***」 */
    int start() default 0;

    /** 用在字符串类型时要脱敏的尾字符位, 比如字符串是「123456」, start 是 2, 当前值是 2, 则最后脱敏成「12 *** 56」 */
    int end() default 0;

    /** 用在 Number 类型, 比如当前值是 100, 则最后脱敏成 0 ~ 99 之间的数, 设置为 0 则表示使用原值 */
    double randomNumber() default 0D;

    /** 用在 Number 类型时的浮点数保留的小数位 */
    int digitsNumber() default 2;

    /** 用在 Date 类型, 比如日期是「2022-01-01」, 当前值是 1234567, 则最后脱敏成「2020-01-01 - (0 ~ 1234566 之间的随机数)」, 设置为 0 则表示使用原值 */
    long randomDateTimeMillis() default 0L;
}
