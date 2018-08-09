package com.github.common.converter;

import com.github.common.util.U;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.util.NumberUtils;

/**
 * 转换数字出错时, 使用 0 代替(默认出错时会抛出 IllegalArgumentException 异常).
 *
 * 此 convert 用于替代 org.springframework.core.convert.support.StringToNumberConverterFactory
 *
 * @see org.springframework.core.convert.support.StringToNumberConverterFactory
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
                try {
                    return NumberUtils.parseNumber(source.trim(), this.targetType);
                } catch (IllegalArgumentException e) {
                    // ignore exception
                }
            }
            return NumberUtils.parseNumber(DEFAULT_VALUE, this.targetType);
        }
    }
}
