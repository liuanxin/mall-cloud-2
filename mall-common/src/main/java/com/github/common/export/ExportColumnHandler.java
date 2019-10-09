package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.collect.Maps;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

final class ExportColumnHandler {

    /** 从类上收集导出的标题(在字段上标的 &#064;ExportColumn 注解) */
    static LinkedHashMap<String, String> collectTitle(Class clazz) {
        LinkedHashMap<String, String> titleMap = Maps.newLinkedHashMap();
        if (U.isNotBlank(clazz)) {
            Field[] fields = clazz.getDeclaredFields();
            if (A.isNotEmpty(fields)) {
                for (Field field : fields) {
                    String name = field.getName();
                    String value = field.getName();
                    ExportColumn column = field.getAnnotation(ExportColumn.class);
                    if (U.isNotBlank(column)) {
                        String columnValue = column.value();
                        if (U.isNotBlank(columnValue)) {
                            value = columnValue;
                        }
                    }
                    titleMap.put(name, value);
                }
            }
        }
        return titleMap;
    }
}
