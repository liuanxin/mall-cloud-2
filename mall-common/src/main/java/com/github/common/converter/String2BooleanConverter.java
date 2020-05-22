package com.github.common.converter;

import org.springframework.core.convert.converter.Converter;

import java.util.HashSet;
import java.util.Set;

/**
 * 字符串转换为 Boolean 类型, 传 true 1 on yes 才是 true, 否则是 false
 * spring 自带的实现中, 只有几种才是 false, 不传将是 null, 转换出错会抛异常.
 *
 * @see org.springframework.core.convert.support.StringToBooleanConverter
 */
public class String2BooleanConverter implements Converter<String, Boolean> {

    private static final Set<String> TRUES = new HashSet<>(4);
    static {
        TRUES.add("true");
        TRUES.add("1");
        TRUES.add("on");
        TRUES.add("yes");
    }

    @Override
    public Boolean convert(String source) {
        String value = source.trim();
        if (value.isEmpty()) {
            return Boolean.FALSE;
        } else {
            value = value.toLowerCase();
            if (TRUES.contains(value)) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }
    }
}
