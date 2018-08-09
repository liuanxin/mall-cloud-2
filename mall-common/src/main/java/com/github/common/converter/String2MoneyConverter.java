package com.github.common.converter;

import com.github.common.Money;
import org.springframework.core.convert.converter.Converter;

/** 将前台传过来的金额转换成 Money 对象.当转换出错时, 将会抛出异常 */
public class String2MoneyConverter implements Converter<String, Money> {

    @Override
    public Money convert(String source) {
        return new Money(source);
    }
}
