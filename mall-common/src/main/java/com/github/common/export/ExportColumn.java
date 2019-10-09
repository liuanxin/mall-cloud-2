package com.github.common.export;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExportColumn {

    /**
     * <pre>
     * 标注在类字段上, 表示这个字段导出时的列名
     * 格式: 标题说明|格式|宽度
     *
     * 格式:「￥ * #,##0.00 ~ r」用 ~ 隔开, 前者表示格式, 后者 r 表示右对齐, 不设置则是左对齐
     *   想 1234.5 显示成 ￥ 1,234.50(金额符号在左) 可以用「￥ * #,##0.00」, 注意 * 的前后各有一个空格
     *   想 1234.5 显示成 ￥ 1,234.50 可以用「￥ #,##0.00」
     *   想 1234.5 显示成 1,234.50 可以用「#,##0.00」
     *   想 12345 显示成 12,345 可以用「#,##0」
     *   想时间 Date 显示成 2001-02-03 04:05:06 可以用「yyyy-MM-dd HH:mm:ss」
     *
     * 宽度: 只接受 >= 1 且 <= 255 的数值, 默认是 12
     * </pre>
     */
    String value();
}
