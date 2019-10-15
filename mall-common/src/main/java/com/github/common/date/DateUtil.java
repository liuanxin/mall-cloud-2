package com.github.common.date;

import com.github.common.util.U;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = 365 * DAY;

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
    /** 格式化日期 yyyy-MM-dd HH:mm:ss SSS */
    public static String formatMs(Date date) {
        return format(date, DateFormatType.YYYY_MM_DD_HH_MM_SS_SSS);
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
            for (DateFormatType type : DateFormatType.values()) {
                Date date = parse(source, type);
                if (U.isNotBlank(date)) {
                    return date;
                }
            }
        }
        return null;
    }
    public static Date parse(String source, DateFormatType type) {
        if (U.isNotBlank(source)) {
            source = source.trim();
            try {
                Date date;
                if (type.isCst()) {
                    // cst 单独处理
                    date = new SimpleDateFormat(type.getValue(), Locale.ENGLISH).parse(source);
                } else {
                    date = parse(source, type.getValue());
                }
                if (date != null) {
                    return date;
                }
            } catch (ParseException | IllegalArgumentException e) {
                // ignore
            }
        }
        return null;
    }
    public static Date parse(String source, String type) {
        if (U.isNotBlank(source)) {
            source = source.trim();
            try {
                Date date = DateTimeFormat.forPattern(type).parseDateTime(source).toDate();
                if (date != null) {
                    return date;
                }
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        return null;
    }

    /** 如: toHuman(36212711413L) ==> 1 年 54 天 3 小时 5 分 11 秒 413 毫秒 */
    public static String toHuman(long intervalMs) {
        if (intervalMs == 0) {
            return U.EMPTY;
        }

        boolean flag = (intervalMs < 0);
        long ms = flag ? -intervalMs : intervalMs;

        long year = ms / YEAR;
        long y = ms % YEAR;

        long day = y / DAY;
        long d = y % DAY;

        long hour = d / HOUR;
        long h = d % HOUR;

        long minute = h / MINUTE;
        long mi = h % MINUTE;

        long second = mi / SECOND;
        long m = mi % SECOND;

        StringBuilder sbd = new StringBuilder();
        if (flag) {
            sbd.append("-");
        }
        if (year != 0) {
            sbd.append(year).append(" 年 ");
        }
        if (day != 0) {
            sbd.append(day).append(" 天 ");
        }
        if (hour != 0) {
            sbd.append(hour).append(" 小时 ");
        }
        if (minute != 0) {
            sbd.append(minute).append(" 分 ");
        }
        if (second != 0) {
            sbd.append(second).append(" 秒 ");
        }
        if (m != 0) {
            sbd.append(m).append(" 毫秒");
        }
        return sbd.toString().trim();
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

    /** 获取一个日期所在星期天(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static Date getSundayStart(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .plusWeeks(-1)
                        .dayOfWeek().withMaximumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在星期六(星期六是最后一天)的最后一毫秒(23:59:59 999) */
    public static Date getSaturdayEnd(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(6)
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在星期一(星期一是第一天)的第一毫秒(00:00:00 000) */
    public static Date getMondayStart(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(1)
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在星期天(星期天是最后一天)的最后一毫秒(23:59:59 999) */
    public static Date getSundayEnd(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .withDayOfWeek(7)
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在月的第一毫秒(00:00:00 000) */
    public static Date getMonthStart(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .dayOfMonth().withMinimumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在月的最后一毫秒(23:59:59 999) */
    public static Date getMonthEnd(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .dayOfMonth().withMaximumValue()
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在季度的第一毫秒(00:00:00 000) */
    public static Date getQuarterStart(Date date) {
        if (U.isBlank(date)) {
            return null;
        }

        DateTime dateTime = new DateTime(date);
        int month = dateTime.getMonthOfYear();
        // 日期所在月的季度开始月
        int quarterMonth = (month % 3 != 0) ? ((month / 3) * 3 + 1) : (month - 2);

        return dateTime.monthOfYear().setCopy(quarterMonth)
                .dayOfMonth().withMinimumValue()
                .hourOfDay().withMinimumValue()
                .minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue()
                .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在季度的最后一毫秒(23:59:59 999) */
    public static Date getQuarterEnd(Date date) {
        if (U.isBlank(date)) {
            return null;
        }
        DateTime dateTime = new DateTime(date);
        int month = dateTime.getMonthOfYear();
        // 日期所在月的季度结束月
        int quarterMonth = (month % 3 != 0) ? (((month / 3) + 1) * 3) : month;

        return dateTime.monthOfYear().setCopy(quarterMonth)
                .dayOfMonth().withMaximumValue()
                .hourOfDay().withMaximumValue()
                .minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue()
                .millisOfSecond().withMaximumValue().toDate();
    }

    /** 获取一个日期所在年的第一毫秒(23:59:59 999) */
    public static Date getYearStart(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .monthOfYear().withMinimumValue()
                        .dayOfMonth().withMinimumValue()
                        .hourOfDay().withMinimumValue()
                        .minuteOfHour().withMinimumValue()
                        .secondOfMinute().withMinimumValue()
                        .millisOfSecond().withMinimumValue().toDate();
    }
    /** 获取一个日期所在年的最后一毫秒(23:59:59 999) */
    public static Date getYearEnd(Date date) {
        return U.isBlank(date) ? null :
                new DateTime(date)
                        .monthOfYear().withMaximumValue()
                        .dayOfMonth().withMaximumValue()
                        .hourOfDay().withMaximumValue()
                        .minuteOfHour().withMaximumValue()
                        .secondOfMinute().withMaximumValue()
                        .millisOfSecond().withMaximumValue().toDate();
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

    /** 计算两个日期之间相差的秒数. 如果 start 比 end 大将会返回负数 */
    public static int betweenSecond(Date start, Date end) {
        if (U.isBlank(start) || U.isBlank(end)) {
            return 0;
        }
        return Seconds.secondsBetween(new DateTime(start), new DateTime(end)).getSeconds();
    }
}
