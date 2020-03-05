package com.github.common.converter;

import com.github.common.util.U;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 转换数字出错时, 使用 0 代替(默认出错时会抛出 IllegalArgumentException 异常).
 *
 * 此 convert 用于替代 {@link org.springframework.core.convert.support.StringToNumberConverterFactory}
 */
public class StringToNumberConverter implements ConverterFactory<String, Number> {

    private static final String DEFAULT_VALUE = "0";

    @Override
    public <T extends Number> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToNumber<>(targetType);
    }

    private static final class StringToNumber<T extends Number> implements Converter<String, T> {
        private final Class<T> targetType;
        StringToNumber(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public T convert(String source) {
            if (U.isNotBlank(source)) {
                // 如果传 1,234,567 将转成 1234567 这样的整数
                String tmp = source.contains(",") ? source.replaceAll(",", "") : source;
                try {
                    return NumberUtils.parseNumber(tmp.trim(), this.targetType);
                } catch (IllegalArgumentException ignore) {
                }
            }
            return NumberUtils.parseNumber(DEFAULT_VALUE, this.targetType);
        }
    }
}
