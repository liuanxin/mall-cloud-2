package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 文件导出. 在 Controller 中调用. 如下示例
 *
 * &#064;Getter
 * &#064;Setter
 * public class XX {
 *     &#064;ExportColumn("名称")
 *     private String name;
 *
 *     &#064;ExportColumn("金额|0.00|20")
 *     private Integer num; // 说明/样式/列宽(1 ~ 255)
 *
 *     &#064;ExportColumn("数量")
 *     private Integer num;
 *
 *     &#064;ExportColumn(value = "时间", dateFormat = DateFormatType.YYYY_MM_DD)
 *     private Date time;   // 时间导出时不定义格式则默认是 yyyy-MM-dd HH:mm:ss
 *
 *     &#064;ExportColumn("类型")
 *     private XXType type; // 枚举导出时会调用其 getValue 方法, 没有值则使用枚举的 name
 * }
 *
 *
 * &#064;GetMapping("/xx-export")
 * public void xxx(String type, ..., HttpServletResponse response) throws IOException {
 *     // 查询要导出的数据
 *     List&lt;XX&gt; xxList = xxService.xxx(...);
 *
 *     // {@link WebExport#export(String, String, List, Class, HttpServletResponse)}
 *     WebExport.export(type, "文件名", xxList, XX.class, response);
 *     // 其中 type 可以是 xls03、xls07、csv 三种(忽略大小写)
 *
 *     // 如果不想在实体中使用 ExportColumn 注解, 可以自己构建一个 {"字段名": "标题"} 的 map,
 *     // 调用 {@link WebExport#export(String, String, LinkedHashMap, List, HttpServletResponse)} 即可
 * }
 * </pre>
 */
public final class WebExport {

    /**
     * 导出文件! 在 Controller 中调用!
     *
     * @param type 文件类型, 现在有 xls03、xls07、csv 三种, 不在这三种中则默认是 xls07
     * @param name 导出时的文件名
     * @param dataList 导出的数据(数组中的每个 object 都是一行, 并且每个字段上有使用 &#064;ExportColumn 注解来说明导出的列名)
     * @param clazz 导出的实体类. 主要用来获取标题头
     */
    public static <T> void export(String type, String name, List<T> dataList, Class<T> clazz,
                                  HttpServletResponse response) throws IOException {
        export(type, name, ExportColumnHandler.collectTitle(clazz), dataList, response);
    }

    /**
     * 导出文件! 在 Controller 中调用!
     *
     * @param type 文件类型, 现在有 xls03、xls07、csv 三种, 不在这三种中则默认是 xls07
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为英文, value 为标题内容)
     * @param dataList 导出的数据(数组中的每个 object 都是一行, object 中的属性名与标题中的 key 相对)
     */
    public static void export(String type, String name, LinkedHashMap<String, String> titleMap,
                              List<?> dataList, HttpServletResponse response) throws IOException {
        ExportType exportType = ExportType.to(type);
        if (exportType.isExcel()) {
            exportExcel(type, name, A.maps(name, titleMap), A.linkedMaps(name, dataList), response);
        } else if (exportType.isCsv()) {
            exportCsv(name, titleMap, dataList, response);
        }
    }

    /**
     * 导出 csv 格式文件
     *
     * @param name     导出的文件名(不带后缀)
     * @param titleMap 标题(key 为英文, value 为标题内容)
     * @param dataList 导出的数据(数组中的每个 object 都是一行, object 中的属性名与标题中的 key 相对)
     */
    private static void exportCsv(String name, LinkedHashMap<String, String> titleMap,
                                  List<?> dataList, HttpServletResponse response) throws IOException {
        // 导出的文件名
        String fileName = encodeName(name) + ".csv";
        typeAndHeader(response, "text/csv", fileName);

        // 没有数据或没有标题, 返回一个内容为空的文件
        String content = ExportCsv.getContent(titleMap, dataList);
        response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 导出 excel 文件(多 sheet)! 在 Controller 中调用!
     *
     * @param type 文件类型: xls03、xls07, 默认是 xls07
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为 sheet 名, value 为每个 sheet 的标题头数据)
     * @param dataList key 为 sheet 名, value 为每个 sheet 导出的数据(数据中的字段名 与 标题头数据 对应)
     */
    public static void exportExcel(String type, String name, Map<String, LinkedHashMap<String, String>> titleMap,
                                   LinkedHashMap<String, List<?>> dataList, HttpServletResponse response) throws IOException {
        boolean excel07 = !(ExportType.to(type).is03());
        // 导出的文件名
        String fileName = encodeName(name) + "." + (excel07 ? "xlsx" : "xls");
        typeAndHeader(response, "text/xls", fileName);

        ExportExcel.handle(excel07, titleMap, dataList).write(response.getOutputStream());
    }

    private static void typeAndHeader(HttpServletResponse response, String type, String fileName) {
        response.setContentType("application/octet-stream; charset=utf-8");
        response.setContentType(type);
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
    }

    private static String encodeName(String name) {
        String fileName = name + "-" + (new SimpleDateFormat("yyMMdd-HHmmss").format(new Date()));
        String userAgent = RequestUtils.userAgent();
        if (U.isNotBlank(userAgent) && userAgent.contains("Mozilla")) {
            // Chrome, Firefox, Safari etc...
            return new String(fileName.getBytes(), StandardCharsets.ISO_8859_1);
        } else {
            return U.urlEncode(fileName);
        }
    }
}
