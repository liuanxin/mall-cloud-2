package com.github.common.converter;

import com.github.common.date.DateFormatType;
import com.github.common.date.DateUtil;
import org.springframework.core.convert.converter.Converter;

import java.util.Date;

/**
 * 字符串转换为日期对象. 匹配多种形式!<br>
 * 前台传过来的不管是 yyyy-MM-dd HH:mm:ss 还是 yyyy-MM-dd 都能转换成 Date 对象.<br>
 * 全部的格式参见 DateFormatType, 当没有匹配时返回 null 而不是抛出异常.<br><br>
 *
 * 此 convert 用于替代 org.springframework.format.annotation.DateTimeFormat 注解
 *
 * @see DateFormatType
 * @see org.springframework.format.annotation.DateTimeFormat
 */
public class StringToDateConverter implements Converter<String, Date> {

    @Override
    public Date convert(String source) {
        return DateUtil.parse(source.trim());
    }
}
