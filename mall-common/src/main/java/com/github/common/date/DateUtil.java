package com.github.common.date;

import com.github.common.util.U;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    /** 当前时间 */
    public static Date now() {
        return new Date();
    }

    /** 返回 yyyy-MM-dd HH:mm:ss 格式的当前时间 */
    public static String nowTime() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }
    /** 返回 yyyy-MM-dd HH:mm:ss SSS 格式的当前时间 */
    public static String nowTimeMs() {
        return now(DateFormatType.YYYY_MM_DD_HH_MM_SS_SSS);
    }
    /** 获取当前时间日期的字符串 */
    public static String now(DateFormatType dateFormatType) {
        return format(now(), dateFormatType);
    }
    /** 格式化日期 yyyy-MM-dd */
    public static String formatDate(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD);
    }
    /** 格式化日期 yyyy/MM/dd */
    public static String formatUsaDate(Date date) {
        return format(date, DateFormatType.USA_YYYY_MM_DD);
    }
    /** 格式化时间 HH:mm:ss */
    public static String formatTime(Date date) {
        return format(date, DateFormatType.HH_MM_SS);
    }
    /** 格式化日期和时间 yyyy-MM-dd HH:mm:ss */
    public static String formatFull(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS);
    }

    /** 格式化日期对象成字符串 */
    public static String format(Date date, DateFormatType type) {
        return (U.isBlank(date) || U.isBlank(type)) ? U.EMPTY : format(date, type.getValue());
    }

    public static String format(Date date, String type) {
        return DateTimeFormat.forPattern(type).print(date.getTime());
    }

    /**
     * 将字符串转换成 Date 对象. 类型基于 DateFormatType 一个一个试. cst 格式麻烦一点
     *
     * @see DateFormatType
     */
    public static Date parse(String source) {
        if (U.isNotBlank(source)) {
            source = source.trim();
            for (DateFormatType type : DateFormatType.values()) {
                try {
                    if (type.isCst()) {
                        // cst 单独处理
                        return new SimpleDateFormat(type.getValue(), Locale.ENGLISH).parse(source);
                    } else {
                        Date date = DateTimeFormat.forPattern(type.getValue()).parseDateTime(source).toDate();
                        if (date != null) {
                            return date;
                        }
                    }
                } catch (ParseException | IllegalArgumentException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    /** 获取一个日期所在天的最开始的时间(00:00:00 000), 对日期查询尤其有用 */
    public static Date getDayStart(Date date) {
        return U.isBlank(date) ? null : getDateTimeStart(date).toDate();
    }
    private static DateTime getDateTimeStart(Date date) {
        return new DateTime(date)
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue();
    }
    /** 获取一个日期所在天的最晚的时间(23:59:59 999), 对日期查询尤其有用 */
    public static Date getDayEnd(Date date) {
        return U.isBlank(date) ? null : getDateTimeEnd(date).toDate();
    }
    private static DateTime getDateTimeEnd(Date date) {
        return new DateTime(date)
                .hourOfDay().withMaximumValue()
                .minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue()
                .millisOfSecond().withMaximumValue();
    }

    /**
     * 取得指定日期 N 天后的日期
     *
     * @param year 正数表示多少年后, 负数表示多少年前
     */
    public static Date addYears(Date date, int year) {
        return new DateTime(date).plusYears(year).toDate();
    }
    /**
     * 取得指定日期 N 个月后的日期
     *
     * @param month 正数表示多少月后, 负数表示多少月前
     */
    public static Date addMonths(Date date, int month) {
        return new DateTime(date).plusMonths(month).toDate();
    }
    /**
     * 取得指定日期 N 天后的日期
     *
     * @param day 正数表示多少天后, 负数表示多少天前
     */
    public static Date addDays(Date date, int day) {
        return new DateTime(date).plusDays(day).toDate();
    }
    /**
     * 取得指定日期 N 周后的日期
     *
     * @param week 正数表示多少周后, 负数表示多少周前
     */
    public static Date addWeeks(Date date, int week) {
        return new DateTime(date).plusWeeks(week).toDate();
    }
    /**
     * 取得指定日期 N 小时后的日期
     *
     * @param hour 正数表示多少小时后, 负数表示多少小时前
     */
    public static Date addHours(Date date, int hour) {
        return new DateTime(date).plusHours(hour).toDate();
    }
    /**
     * 取得指定日期 N 分钟后的日期
     *
     * @param minute 正数表示多少分钟后, 负数表示多少分钟前
     */
    public static Date addMinute(Date date, int minute) {
        return new DateTime(date).plusMinutes(minute).toDate();
    }
    /**
     * 取得指定日期 N 秒后的日期
     *
     * @param second 正数表示多少秒后, 负数表示多少秒前
     */
    public static Date addSeconds(Date date, int second) {
        return new DateTime(date).plusSeconds(second).toDate();
    }

    /** 传入的时间是不是当月当日. 用来验证生日 */
    public static boolean wasBirthday(Date date) {
        DateTime dt = DateTime.now();
        DateTime dateTime = new DateTime(date);
        return dt.getMonthOfYear() == dateTime.getMonthOfYear() && dt.getDayOfMonth() == dateTime.getDayOfMonth();
    }

    /** 计算两个日期之间相差的天数. 如果 start 比 end 大将会返回负数 */
    public static int betweenDay(Date start, Date end) {
        if (U.isBlank(start) || U.isBlank(end)) {
            return 0;
        }
        return Days.daysBetween(getDateTimeStart(start), getDateTimeStart(end)).getDays();
    }
}
