package com.github.common.converter;

import com.github.common.util.U;
import org.springframework.core.convert.converter.Converter;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;

/** 前台传递过来的参数去左右空格, 并进行转义(过滤 XSS 攻击) */
public class StringTrimAndEscapeConverter implements Converter<String, String> {

    @Override
    public String convert(String source) {
        if (source == null) {
            return U.EMPTY;
        }

        return HtmlUtils.htmlEscape(source.trim(), StandardCharsets.UTF_8.name());
    }
}
