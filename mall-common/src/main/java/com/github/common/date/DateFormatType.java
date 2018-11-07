package com.github.common.date;

/** 日期的格式化类型 */
public enum DateFormatType {

    /** yyyy-MM-dd HH:mm:ss SSS */
    YYYY_MM_DD_HH_MM_SS_SSS("yyyy-MM-dd HH:mm:ss SSS"),
    /** yyyy-MM-dd HH:mm:ss */
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    /** yyyy-MM-dd HH:mm */
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
    /** yyyy-MM-dd */
    YYYY_MM_DD("yyyy-MM-dd"),
    /** yyyy-MM */
    YYYY_MM("yyyy-MM"),
    /** yyyy-MM-dd am/pm --> am/pm 会根据时区自动完成, 也就是如果当前时区是北京的话, 会显示成 上午/下午 */
    YYYY_MM_DD_AP("yyyy-MM-dd a"),

    /** yyyyMMddHHmmssSSS */
    YYYYMMDDHHMMSSSSS("yyyyMMddHHmmssSSS"),
    /** yyyyMMddHHmmss */
    YYYYMMDDHHMMSS("yyyyMMddHHmmss"),
    /** yyMMddHHmmss */
    YYMMDDHHMMSS("yyMMddHHmmss"),
    /** yyyyMMddHHmm */
    YYYYMMDDHHMM("yyyyMMddHHmm"),
    /** yyMMddHHmm */
    YYMMDDHHMM("yyMMddHHmm"),
    /** yyyyMMdd */
    YYYYMMDD("yyyyMMdd"),
    /** yyMMdd */
    YYMMDD("yyMMdd"),
    /** yyyyMM */
    YYYYMM("yyyyMM"),

    /** HH:mm:ss */
    HH_MM_SS("HH:mm:ss"),
    /** HH:mm */
    HH_MM("HH:mm"),

    /** 到毫秒: yyyy-MM-ddTHH:mm:ss.SSSZ */
    TSZ("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
    /** 到毫秒: yyyy-MM-ddTHH:mm:ss.SSS */
    TS("yyyy-MM-dd'T'HH:mm:ss.SSS"),
    /** 到秒: yyyy-MM-ddTHH:mm:ssZ */
    TZ("yyyy-MM-dd'T'HH:mm:ss'Z'"),
    /** 到秒: yyyy-MM-ddTHH:mm:ss */
    T("yyyy-MM-dd'T'HH:mm:ss"),

    /** yyyy/MM/dd */
    USA_YYYY_MM_DD("yyyy/MM/dd"),
    /** MM/dd/yyyy HH:mm:ss */
    USA_MM_DD_YYYY_HH_MM_SS("MM/dd/yyyy HH:mm:ss"),
    /** yyyy年MM月dd日 HH时mm分ss秒 */
    CN_YYYY_MM_DD_HH_MM_SS("yyyy年MM月dd日 HH时mm分ss秒"),
    /** yyyy年MM月dd日 HH点 */
    CN_YYYY_MM_DD_HH("yyyy年MM月dd日 HH点"),
    /** yyyy年MM月dd日 HH点 */
    CN_YYYY_MM_DD_HH_MM("yyyy年MM月dd日 HH点mm分"),
    /** yyyy年MM月dd日 */
    CN_YYYY_MM_DD("yyyy年MM月dd日"),

    /** 直接打印 new Date() 时的样式 */
    CST("EEE MMM dd HH:mm:ss zzz yyyy");

    private String value;
    DateFormatType(String value) { this.value = value; }
    public String getValue() { return value; }

    public boolean isCst() {
        return this == CST;
    }
}
