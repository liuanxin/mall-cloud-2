package com.github.common.export;

import com.github.common.export.csv.ExportCsv;
import com.github.common.export.easy.ExportEasyExcel;
import com.github.common.export.poi.ExportColumnHandler;
import com.github.common.export.poi.ExportExcel;
import com.github.common.util.A;
import com.github.common.util.U;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
 * // {@link #save(String, String, List, Class, String)}
 * FileExport.save(type, "文件名", xxList, XX.class, directory);
 * // 其中 type 可以是 xls03、xls07、csv 三种(忽略大小写)
 *
 * // 如果不想在实体中使用 ExportColumn 注解, 可以自己构建一个 {"字段名": "标题"} 的 map,
 * // 调用 {@link #save(String, String, LinkedHashMap, List, boolean, String)} 即可
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
        save(type, name, ExportColumnHandler.collectTitle(clazz), dataList, false, directory);
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
                            List<?> dataList, boolean primitivePoi, String directory) {
        ExportType exportType = ExportType.to(type);
        if (exportType.isExcel()) {
            if (primitivePoi) {
                saveExcel(type, name, A.linkedMaps(name, titleMap), A.linkedMaps(name, dataList), directory);
            } else {
                saveEasyExcel(type, name, A.linkedMaps(name, titleMap), A.linkedMaps(name, dataList), directory);
            }
        } else if (exportType.isCsv()) {
            saveCsv(name, titleMap, dataList, directory);
        }
    }

    /**
     * 保存 csv 格式文件, 文件将保存到指定目录
     *
     * @param name     导出的文件名(不带后缀)
     * @param titleMap 标题(key 为英文, value 为标题内容)
     * @param dataList 导出的数据(数组中的每个 object 都是一行, object 中的属性名与标题中的 key 相对)
     * @param directory 文件保存的目录
     */
    private static void saveCsv(String name, LinkedHashMap<String, String> titleMap,
                                List<?> dataList, String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        // 没有数据或没有标题, 返回一个内容为空的文件
        byte[] content = ExportCsv.getContent(titleMap, dataList).getBytes(StandardCharsets.UTF_8);
        String fileName = encodeName(name) + ".csv";
        try {
            Files.write(new File(dir, fileName).toPath(), content, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException(String.format("保存文件(%s)到(%s)时异常", fileName, directory), e);
        }
    }

    /**
     * 导出 excel 文件(多 sheet), 文件将保存到指定目录
     *
     * @param type 文件类型: xls03、xls07, 默认是 xlsx
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为 sheet 名, value 为每个 sheet 的标题头数据)
     * @param dataList key 为 sheet 名, value 为每个 sheet 导出的数据(数据中的字段名 与 标题头数据 对应)
     * @param directory 文件保存的目录
     */
    public static void saveExcel(String type, String name, Map<String, LinkedHashMap<String, String>> titleMap,
                                 LinkedHashMap<String, List<?>> dataList, String directory) {
        boolean excel07 = !(ExportType.to(type).is03());
        String fileName = encodeName(name) + "." + (excel07 ? "xlsx" : "xls");

        try (
                OutputStream output = new FileOutputStream(U.addSuffix(directory) + fileName);
                Workbook workbook = ExportExcel.handle(excel07, titleMap, dataList)
        ) {
            try {
                workbook.write(output);
            } finally {
                ExportExcel.dispose(workbook);
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("保存文件(%s)到(%s)时异常", fileName, directory), e);
        }
    }

    /**
     * 导出 excel 文件(多 sheet), 文件将保存到指定目录
     *
     * @param type 文件类型: XLS, XLSX, CSV    分别表示 03 07 格式的文件, 默认是 xlsx
     * @param name 导出时的文件名
     * @param titleMap 标题(key 为 sheet 名, value 为每个 sheet 的标题头数据)
     * @param dataList key 为 sheet 名, value 为每个 sheet 导出的数据(数据中的字段名 与 标题头数据 对应)
     * @param directory 文件保存的目录
     */
    public static void saveEasyExcel(String type, String name, LinkedHashMap<String, LinkedHashMap<String, String>> titleMap,
                                     LinkedHashMap<String, List<?>> dataList, String directory) {
        boolean excel07 = !(ExportType.to(type).is03());
        String fileName = encodeName(name) + "." + (excel07 ? "xlsx" : "xls");

        try (OutputStream output = new FileOutputStream(U.addSuffix(directory) + fileName)) {
            ExportEasyExcel.handle(excel07, titleMap, dataList, output);
        } catch (IOException e) {
            throw new RuntimeException(String.format("保存文件(%s)到(%s)时异常", fileName, directory), e);
        }
    }

    /** 将多个文件压缩成一个 zip 文件 */
    public static void writeZipFile(Set<File> files, File zipFile) {
        try (
                FileOutputStream fileStream = new FileOutputStream(zipFile);
                ZipOutputStream outputStream = new ZipOutputStream(fileStream)
        ) {
            for (File file : files) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    outputStream.putNextEntry(new ZipEntry(file.getName()));
                    // jdk-9
                    // inputStream.transferTo(outputStream);
                    byte[] buf = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, length);
                    }
                    // guava
                    // ByteStreams.copy(inputStream, outputStream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("文件(%s)压缩成(%s)时异常", files, zipFile), e);
        }
    }

    private static String encodeName(String name) {
        return name + "-" + (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
    }
}
