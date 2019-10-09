package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.U;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 保存到文件. 如下示例
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
 * // 查询要保存的数据
 * List&lt;XX&gt; xxList = xxService.xxx(...);
 *
 * // {@link FileExport#save(String, String, List, Class, String)}
 * FileExport.save(type, "文件名", xxList, XX.class, directory);
 * // 其中 type 可以是 xls03、xls07、csv 三种(忽略大小写)
 *
 * // 如果不想在实体中使用 ExportColumn 注解, 可以自己构建一个 {"字段名": "标题"} 的 map,
 * // 调用 {@link FileExport#save(String, String, LinkedHashMap, List, String)} 即可
 * </pre>
 */
public final class FileExport {

    /**
     * 保存文件
     *
     * @param type 文件类型, 现在有 xls03、xls07、csv 三种, 不在这三种中则默认是 xls07
     * @param name 导出时的文件名
     * @param dataList 导出的数据(数组中的每个 object 都是一行, 并且每个字段上有使用 &#064;ExportColumn 注解来说明导出的列名)
     * @param clazz 导出的实体类. 主要用来获取标题头
     * @param directory 文件保存的目录
     */
    public static <T> void save(String type, String name, List<T> dataList, Class<T> clazz, String directory) {
        save(type, name, ExportColumnHandler.collectTitle(clazz), dataList, directory);
    }

    /**
     * 保存文件
     *
     * @param type 文件类型, 现在有 xls03、xls07、csv 三种, 不在这三种中则默认是 xls07
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为英文, value 为标题内容)
     * @param dataList 导出的数据(数组中的每个 object 都是一行, object 中的属性名与标题中的 key 相对)
     * @param directory 文件保存的目录
     */
    public static void save(String type, String name, LinkedHashMap<String, String> titleMap,
                            List<?> dataList, String directory) {
        ExportType exportType = ExportType.to(type);
        if (exportType.isExcel()) {
            saveExcel(type, name, A.linkedMaps(name, titleMap), A.linkedMaps(name, dataList), directory);
        } else if (exportType.isCsv()) {
            saveCsv(name, titleMap, dataList, directory);
        }
    }

    /**
     * 保存 csv 格式文件
     *
     * @param name     导出的文件名(不带后缀)
     * @param titleMap 标题(key 为英文, value 为标题内容)
     * @param dataList 导出的数据(数组中的每个 object 都是一行, object 中的属性名与标题中的 key 相对)
     */
    private static void saveCsv(String name, LinkedHashMap<String, String> titleMap,
                                List<?> dataList, String directory) {
        String fileName = encodeName(name) + ".csv";

        // 没有数据或没有标题, 返回一个内容为空的文件
        String content = ExportCsv.getContent(titleMap, dataList);

        try (OutputStream output = new FileOutputStream(U.addSuffix(directory) + fileName)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(String.format("保存文件(%s)到(%s)时异常", fileName, directory), e);
        }
    }

    /**
     * 导出 excel 文件(多 sheet)! 在 Controller 中调用!
     *
     * @param type 文件类型: xls03、xls07, 默认是 xls07
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为 sheet 名, value 为每个 sheet 的标题头数据)
     * @param dataList key 为 sheet 名, value 为每个 sheet 导出的数据(数据中的字段名 与 标题头数据 对应)
     */
    public static void saveExcel(String type, String name, Map<String, LinkedHashMap<String, String>> titleMap,
                                 LinkedHashMap<String, List<?>> dataList, String directory) {
        boolean excel07 = !(ExportType.to(type).is03());
        String fileName = encodeName(name) + "." + (excel07 ? "xlsx" : "xls");

        try (
                OutputStream output = new FileOutputStream(U.addSuffix(directory) + fileName);
                Workbook workbook = ExportExcel.handle(excel07, titleMap, dataList);
        ) {
            workbook.write(output);
        } catch (IOException e) {
            throw new RuntimeException(String.format("保存文件(%s)到(%s)时异常", fileName, directory), e);
        }
    }

    private static String encodeName(String name) {
        return name + "-" + (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
    }
}
