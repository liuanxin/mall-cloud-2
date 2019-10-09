package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import org.apache.poi.ss.usermodel.Workbook;

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
     * <pre>
     * 返回一个 excel 工作簿
     *
     * excel07:  是否返回 microsoft excel 2007 的版本
     *
     * titleMap: key 是 sheet name, value 是这个 sheet 对应的标题映射, 标题映射 跟 下面数据的字段名 一一对应, 如
     * {
     *   "用户信息": { "name": "用户", "desc": "说明" },
     *   "商品信息": { "name": "商品名称", "image": "图片" }
     * }
     *
     * dataMap: key 是 sheet name, value 是这个 sheet 的数据, 单条数据的字段名 跟 上面的标题映射 一一对应, 如
     * {
     *   "用户信息": [ { "name": "张三", "desc": "三儿" }, { "name": "李四", "desc": "四儿" } ... ],
     *   "商品信息": [ { "name": "苹果", "image": "xxx" }, { "name": "三星", "image": "yyy" } ]
     * }
     *
     * 最终导出时大概是这样(假定用户信息的数据很多, 此时会有三个 sheet, 前两个是用户信息, 后一个是商品信息)
     *
     * | 用户    说明    |    用户    说明    |    商品名称    图片 |
     * | 张三    三儿    |    王五    王儿    |    苹果       xxx  |
     * | 李四    四儿    |    钱六    六儿    |    三星       yyy  |
     * |                |                   |                   |
     * | 用户信息-1      |    用户信息-2      |    商品信息        |
     * </pre>
     */
    public static void exportExcel(String type, String name, Map<String, LinkedHashMap<String, String>> titleMap,
                                   LinkedHashMap<String, List<?>> dataList, HttpServletResponse response) throws IOException {
        boolean excel07 = !(ExportType.to(type).is03());
        // 导出的文件名
        String fileName = encodeName(name) + "." + (excel07 ? "xlsx" : "xls");
        typeAndHeader(response, "application/vnd.ms-excel", fileName);

        try (Workbook workbook = ExportExcel.handle(excel07, titleMap, dataList)) {
            workbook.write(response.getOutputStream());
        }
    }

    private static void typeAndHeader(HttpServletResponse response, String type, String fileName) {
        // response.setContentType("application/octet-stream; charset=utf-8");
        response.setContentType(type);
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
    }

    private static String encodeName(String name) {
        String fileName = name + "-" + (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        String userAgent = RequestUtils.userAgent();
        if (U.isNotBlank(userAgent) && userAgent.contains("Mozilla")) {
            // Chrome, Firefox, Safari etc...
            return new String(fileName.getBytes(), StandardCharsets.ISO_8859_1);
        } else {
            return U.urlEncode(fileName);
        }
    }
}
