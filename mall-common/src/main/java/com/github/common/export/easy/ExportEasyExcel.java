package com.github.common.export.easy;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.handler.WriteHandler;
import com.github.common.util.A;
import com.github.common.util.U;

import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("DuplicatedCode")
public class ExportEasyExcel {

    /** 多空白符的正则 */
    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{2,}");

    /** 总条数超出这个值后 cell 就不再设置样式 */
    private static final int CELL_STYLE_CROSSOVER = 5_000;

    private static final WriteHandler FREEZE_TITLE_HANDLER = new FreezeTitleSheetHandler();

    private static int getMaxColumn(boolean excel07) {
        return excel07 ? 16384 : 256;
    }
    // 2003(xls)  单个 sheet 最多只能有   65536 行   256 列
    // 2007(xlsx) 及以上的版本最多只能有 1048576 行 16384 列
    private static int getMaxRow(boolean excel07) {
        // return excel07 ? 1048576 : 65535;
        return excel07 ? 300_000 : 50_000;
    }


    /**
     * 导出 excel 文件, dataList 如果过大, 超出单个 sheet 的最大行数, 将会拆成多个 sheet
     *
     * @param excel07  是否返回 microsoft excel 2007 的版本
     * @param sheetName sheet 名
     * @param dataList 这个 sheet 的数据, 在具体的字段上标 {@link com.alibaba.excel.annotation.ExcelProperty } 注解
     */
    public static void handle(boolean excel07, String sheetName, List<?> dataList, OutputStream outputStream) {
        // 每个 sheet 的最大行, 标题头也是一行
        int sheetMaxRow = getMaxRow(excel07) - 1;
        ExcelTypeEnum excelType = excel07 ? ExcelTypeEnum.XLSX : ExcelTypeEnum.XLS;
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream).excelType(excelType).build()) {
            int size;
            Class<?> headClass;
            if (A.isEmpty(dataList)) {
                size = 0;
                headClass = Void.class;
            } else {
                size = dataList.size();
                headClass = dataList.get(0).getClass();
            }
            List<WriteHandler> handlerList = Arrays.asList(
                    FREEZE_TITLE_HANDLER,
                    new StyleCellHandler(size <= CELL_STYLE_CROSSOVER)
            );

            // 一个 sheet 数据过多时 excel 处理会出错, 这时候分成多个 sheet 导出
            int sheetCount = (size % sheetMaxRow == 0) ? (size / sheetMaxRow) : (size / sheetMaxRow + 1);
            // 如果没有记录也至少构建一个(确保导出的文件有标题头)
            int realSheetCount = (sheetCount == 0) ? 1 : sheetCount;

            for (int i = 0; i < realSheetCount; i++) {
                String realSheetName = handleSheetName(sheetName, realSheetCount, i);

                int fromIndex, toIndex;
                if (realSheetCount > 1) {
                    fromIndex = sheetMaxRow * i;
                    toIndex = (i + 1 == realSheetCount) ? size : (fromIndex + sheetMaxRow);
                } else {
                    fromIndex = 0;
                    toIndex = size;
                }
                List<?> realDataList = A.isEmpty(dataList) ? Collections.emptyList() : dataList.subList(fromIndex, toIndex);

                // 指定头, 并指定只输出指定头相关的列
                ExcelWriterSheetBuilder sheetBuilder = EasyExcel.writerSheet(realSheetName).useDefaultStyle(false);
                if (headClass != Void.class) {
                    sheetBuilder.head(headClass);
                }
                for (WriteHandler handler : handlerList) {
                    sheetBuilder.registerWriteHandler(handler);
                }
                excelWriter.write(realDataList, sheetBuilder.build());
            }
        }
    }

    /**
     * <pre>
     * 导出 excel 文件.
     *
     * excel07:  是否返回 microsoft excel 2007 的版本
     *
     * titleMap: key 是 sheet name, value 是这个 sheet 对应的标题映射, 标题映射 跟 下面数据的字段名 一一对应, 如
     * {
     *   "用户信息": { "name": "用户", "desc": "说明" },
     *   "商品信息": { "name": "商品名称", "image": "图片" }
     * }
     *
     * dataMap:  key 是 sheet name, value 是这个 sheet 的数据, 单条数据的字段名 跟 上面的标题映射 一一对应, 如
     * {
     *   "用户信息": [ { "name": "张三", "desc": "三儿" }, { "name": "李四", "desc": "四儿" } ... ],
     *   "商品信息": [ { "name": "苹果", "image": "xxx" }, { "name": "三星", "image": "yyy" } ]
     * }
     *
     * 最终导出时大概是这样(假定用户信息的数据很多, 此时会有三个 sheet, 前两个是用户信息, 后一个是商品信息)
     *
     * | 用户    说明    |    用户    说明    |    商品名称    图片 |
     * | 张三    三儿    |    王五    五儿    |    苹果       xxx  |
     * | 李四    四儿    |    钱六    六儿    |    三星       yyy  |
     * |                |                   |                   |
     * | 用户信息-1      |    用户信息-2      |    商品信息        |
     * </pre>
     */
    public static void handle(boolean excel07, LinkedHashMap<String, LinkedHashMap<String, String>> titleMap,
                              LinkedHashMap<String, List<?>> dataMap, OutputStream outputStream) {
        // 每个 sheet 的最大列
        int maxColumn = getMaxColumn(excel07);
        for (LinkedHashMap<String, String> sheetTitleMap : titleMap.values()) {
            if (A.isNotEmpty(sheetTitleMap)) {
                int columnSize = sheetTitleMap.size();
                if (columnSize > maxColumn) {
                    throw new RuntimeException("Invalid column number " + columnSize + ", max: " + maxColumn);
                }
            }
        }

        // 每个 sheet 的最大行, 标题头也是一行
        int sheetMaxRow = getMaxRow(excel07) - 1;
        ExcelTypeEnum excelType = excel07 ? ExcelTypeEnum.XLSX : ExcelTypeEnum.XLS;
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream).excelType(excelType).build()) {
            for (Map.Entry<String, LinkedHashMap<String, String>> entry : titleMap.entrySet()) {
                String sheetName = entry.getKey();
                LinkedHashMap<String, String> sheetTitleMap = entry.getValue();
                // 表头在数据实体中的字段名
                Set<String> fields = sheetTitleMap.keySet();
                // 表头在导出时显示的名字
                List<List<String>> titles = new ArrayList<>();
                for (String value : sheetTitleMap.values()) {
                    if (U.isNotBlank(value)) {
                        titles.add(Collections.singletonList(value));
                    }
                }

                List<?> sheetDataList = dataMap.get(sheetName);
                int size = A.isEmpty(sheetDataList) ? 0 : sheetDataList.size();
                List<WriteHandler> handlerList = Arrays.asList(
                        FREEZE_TITLE_HANDLER,
                        new StyleCellHandler(size <= CELL_STYLE_CROSSOVER)
                );

                // 一个 sheet 数据过多时 excel 处理会出错, 这时候分成多个 sheet 导出
                int sheetCount = (size % sheetMaxRow == 0) ? (size / sheetMaxRow) : (size / sheetMaxRow + 1);
                // 如果没有记录也至少构建一个(确保导出的文件有标题头)
                int realSheetCount = (sheetCount == 0) ? 1 : sheetCount;

                for (int i = 0; i < realSheetCount; i++) {
                    String realSheetName = handleSheetName(sheetName, realSheetCount, i);

                    List<?> realDataList;
                    if (A.isEmpty(sheetDataList)) {
                        realDataList = Collections.emptyList();
                    } else {
                        if (realSheetCount > 1) {
                            int fromIndex = sheetMaxRow * i;
                            int toIndex = (i + 1 == realSheetCount) ? size : (fromIndex + sheetMaxRow);
                            realDataList = sheetDataList.subList(fromIndex, toIndex);
                        } else {
                            realDataList = sheetDataList;
                        }
                    }

                    // 指定头, 并指定只输出指定头相关的列
                    ExcelWriterSheetBuilder sheetBuilder = EasyExcel.writerSheet(realSheetName)
                            .useDefaultStyle(false).includeColumnFieldNames(fields);
                    if (A.isNotEmpty(titles)) {
                        sheetBuilder.head(titles);
                    }
                    for (WriteHandler handler : handlerList) {
                        sheetBuilder.registerWriteHandler(handler);
                    }
                    excelWriter.write(realDataList, sheetBuilder.build());
                }
            }
        }
    }

    /**
     * 将特殊字符替换成空格, 最开始及最结尾是单引号则去掉, 多个空格替换成一个, 如果有多个 sheet 就拼在名字后面, 长度超过 31 则截取
     *
     * @see org.apache.poi.ss.util.WorkbookUtil#validateSheetName
     */
    private static String handleSheetName(String sheetName, int sheetCount, int sheetIndex) {
        if (U.isBlank(sheetName)) {
            sheetName = "sheet";
        }
        String tmpSn = sheetName.replace("/", " ").replace("\\", " ").replace("?", " ")
                .replace("*", " ").replace("]", " ").replace("[", " ").replace(":", " ");
        if (tmpSn.startsWith("'")) {
            tmpSn = tmpSn.substring(1);
        }
        if (tmpSn.endsWith("'")) {
            tmpSn = tmpSn.substring(0, tmpSn.length() - 1);
        }
        String tmp = BLANK_REGEX.matcher(tmpSn).replaceAll(" ");
        String indexSuffix = (sheetCount > 1) ? (" - " + (sheetIndex + 1)) : U.EMPTY;
        int nameLen = 31 - indexSuffix.length();
        return ((tmp.length() > nameLen) ? tmp.substring(0, nameLen) : tmp) + indexSuffix;
    }
}
