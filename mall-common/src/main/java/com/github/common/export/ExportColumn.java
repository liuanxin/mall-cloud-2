package com.github.common.export;

import com.github.common.date.DateFormatType;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExportColumn {

    /** 标注在类字段上, 表示这个字段导出时的列名, 格式: 标题说明|数字格式(比如金额用 0.00)|宽度(255 以内) */
    String value();

    /** 当想要自定义时间类型的格式时使用(只在字段是 {@link java.util.Date} 类型时有用) */
    DateFormatType dateFormat() default DateFormatType.YYYY_MM_DD_HH_MM_SS;
}
