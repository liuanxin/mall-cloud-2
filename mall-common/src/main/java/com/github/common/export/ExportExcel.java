package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.U;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.util.*;
import java.util.regex.Pattern;

/** 如果想要将数据导成文件保持, 使用 {@link FileExport} 类, 如果要导出文件在 web 端下载, 使用 {@link WebExport} 类 */
final class ExportExcel {

    /** 标题行字体大小 */
    private static final short TITLE_FONT_SIZE = 12;
    /** 行字体大小 */
    private static final short FONT_SIZE = 10;

    /** 标题行高 */
    private static final short TITLE_ROW_HEIGHT = 20;
    /** 行高 */
    private static final short ROW_HEIGHT = 18;
    /** 多空白符的正则 */
    private static final Pattern BLANK_REGEX = Pattern.compile("\\s{2,}");

    /** 本地线程中缓存的自定义列样式 */
    private static final ThreadLocal<Map<String, CellStyle>> CUSTOMIZE_CELL_STYLE = new ThreadLocal<>();

    static int getMaxColumn(boolean excel07) {
        return excel07 ? 16384 : 256;
    }
    // 2003(xls)  单个 sheet 最多只能有   65536 行   256 列
    // 2007(xlsx) 及以上的版本最多只能有 1048576 行 16384 列
    static int getMaxRow(boolean excel07) {
        return excel07 ? 1048576 : 65535;
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
    static Workbook handle(boolean excel07, Map<String, LinkedHashMap<String, String>> titleMap,
                           LinkedHashMap<String, List<?>> dataMap) {
        int maxColumn = getMaxColumn(excel07);
        int columnSize = titleMap.size();
        if (columnSize > maxColumn) {
            throw new RuntimeException("Invalid column number " + columnSize + ", max " + maxColumn);
        }

        Workbook workbook = create(excel07);
        // 没有标题直接返回
        if (A.isEmpty(titleMap)) {
            return workbook;
        }
        // 如果数据为空, 构建一个空字典(确保导出的文件有标题头)
        if (dataMap == null) {
            dataMap = new LinkedHashMap<>();
        }

        // 头样式
        CellStyle headStyle = headStyle(workbook);
        // 内容样式
        CellStyle contentStyle = contentStyle(workbook);
        // 数字样式
        CellStyle numberStyle = numberStyle(workbook);
        // 每个列用到的样式
        CellStyle cellStyle;

        Sheet sheet;
        //  行
        Row row;
        //   列
        Cell cell;
        //  表格数       行索引      列索引
        int sheetCount, rowIndex, cellIndex;
        //  数据总条数  数据起始索引  数据结束索引
        int size, fromIndex, toIndex;
        //      数据
        List<?> dataList;

        // 单个列的数据
        String cellData;
        // 标题说明|数字格式(比如金额用 0.00)~r~b|宽度(255 以内) : 中间字段的第二部分的 r 表示右对齐, b 表示粗体
        String[] titleValues;
        // 数字格式
        DataFormat dataFormat = workbook.createDataFormat();

        // 每个 sheet 的最大行, 标题头也是一行
        int sheetMaxRow = getMaxRow(excel07) - 1;
        try {
            for (Map.Entry<String, List<?>> entry : dataMap.entrySet()) {
                String sheetName = entry.getKey();
                // 标题头, 这里跟数据中的属性相对应
                Set<Map.Entry<String, String>> titleEntry = titleMap.get(sheetName).entrySet();

                // 当前 sheet 的数据
                dataList = entry.getValue();
                size = A.isEmpty(dataList) ? 0 : dataList.size();

                // 一个 sheet 数据过多 excel 处理会出错, 分多个 sheet
                sheetCount = (size % sheetMaxRow == 0) ? (size / sheetMaxRow) : (size / sheetMaxRow + 1);
                if (sheetCount == 0) {
                    // 如果没有记录时也至少构建一个(确保导出的文件有标题头)
                    sheetCount = 1;
                }

                for (int i = 0; i < sheetCount; i++) {
                    // 构建 sheet
                    sheet = workbook.createSheet(handleSheetName(sheetName, sheetCount, i));

                    // 每个 sheet 的标题行
                    rowIndex = 0;
                    cellIndex = 0;
                    row = sheet.createRow(rowIndex);
                    row.setHeightInPoints(TITLE_ROW_HEIGHT);
                    for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                        // 标题列
                        cell = row.createCell(cellIndex);
                        cell.setCellStyle(headStyle);
                        cell.setCellValue(U.getNil(titleMapEntry.getValue().split("\\|")[0]));
                        cellIndex++;
                    }
                    // 冻结标题
                    sheet.createFreezePane(0, 1, 0, 1);

                    if (size > 0) {
                        if (sheetCount > 1) {
                            fromIndex = sheetMaxRow * i;
                            toIndex = (i + 1 == sheetCount) ? size : (fromIndex + sheetMaxRow);
                        } else {
                            fromIndex = 0;
                            toIndex = size;
                        }
                        for (int j = fromIndex; j < toIndex; j++) {
                            // 每个 sheet 除标题行以外的数据
                            Object data = dataList.get(j);
                            if (data != null) {
                                rowIndex++;
                                cellIndex = 0;
                                row = sheet.createRow(rowIndex);
                                row.setHeightInPoints(ROW_HEIGHT);
                                for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                                    // 数据列
                                    cell = row.createCell(cellIndex);

                                    cellData = U.getField(data, titleMapEntry.getKey());
                                    titleValues = titleMapEntry.getValue().split("\\|");

                                    boolean isNumber = U.isNumber(cellData);
                                    if (titleValues.length > 1) {
                                        cellStyle = customizeStyle(workbook, titleValues[1], isNumber, dataFormat);
                                    } else {
                                        cellStyle = isNumber ? numberStyle : contentStyle;
                                    }
                                    cell.setCellStyle(cellStyle);

                                    if (isNumber) {
                                        cell.setCellValue(U.toDouble(cellData));
                                    } else {
                                        cell.setCellValue(cellData);
                                    }
                                    cellIndex++;
                                }
                            }
                        }
                    }

                    // 在列上处理宽度
                    cellIndex = 0;
                    for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                        titleValues = titleMapEntry.getValue().split("\\|");
                        if (titleValues.length > 2) {
                            int width = U.toInt(titleValues[2]);
                            // 默认 12
                            int w = (width > 0 && width < 256) ? width : 12;
                            // 左移 8 相当于 * 256
                            sheet.setColumnWidth(cellIndex, w << 8);
                        } else {
                            // 让列的宽度自适应. 缺少中文字体计算宽度时会有问题, 需要复制中文字体文件到操作系统
                            sheet.autoSizeColumn(cellIndex, true);
                        }
                        cellIndex++;
                    }
                }
            }
        } finally {
            // 把本地线程中缓存的自定义列样式清空
            CUSTOMIZE_CELL_STYLE.remove();
        }
        return workbook;
    }
    private static Workbook create(boolean excel07) {
        // 声明一个工作薄. HSSFWorkbook 是 Office 2003 的版本, XSSFWorkbook 是 2007
        //Workbook workbook = excel07 ? new XSSFWorkbook() : new HSSFWorkbook();
        if (excel07) {
            // http://poi.apache.org/components/spreadsheet/how-to.html#sxssf
            // sxssf 会把数据缓存到磁盘来完成 数据量比较大时 的输出
            SXSSFWorkbook workbook = new SXSSFWorkbook();
            // 启用压缩, 临时文件会变小, 但是会消耗 cpu 运算时间
            workbook.setCompressTempFiles(true);
            return workbook;
        } else {
            return new HSSFWorkbook();
        }
    }
    /** 清除临时文件 */
    static void dispose(Workbook workbook) {
        if (U.isNotBlank(workbook) && workbook instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook) workbook).dispose();
        }
    }

    /**
     * 将特殊字符替换成空格, 最开始及最结尾是单引号则去掉, 多个空格替换成一个, 如果有多个 sheet 就拼在名字后面, 长度超过 31 则截取
     *
     * @see org.apache.poi.ss.util.WorkbookUtil#validateSheetName
     */
    private static String handleSheetName(String sheetName, int sheetCount, int sheetIndex) {
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
        return (tmp.length() > nameLen) ? (tmp.substring(0, nameLen) + indexSuffix) : tmp;
    }

    /** 头样式 */
    private static CellStyle headStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 水平居左
        style.setAlignment(HorizontalAlignment.LEFT);
        // 垂直居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        // 粗体
        font.setBold(true);
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        style.setFont(font);
        return style;
    }

    /** 内容样式 */
    private static CellStyle contentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);
        return style;
    }

    /** 数字样式 */
    private static CellStyle numberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);
        return style;
    }

    /**
     * 列如果有自定义样式, 就先从 本地线程的自定义列样式缓存 中取, 有就直接返回, 没有就生成并写进缓存.
     * 这样当一个 excel 行过多时, 相同 style 标识的自定义样式只需要生成一次, 不需要每个列都生成新的
     */
    private static CellStyle customizeStyle(Workbook workbook, String style, boolean isNumber, DataFormat dataFormat) {
        Map<String, CellStyle> currentStyleMap = CUSTOMIZE_CELL_STYLE.get();
        Map<String, CellStyle> tmpStyleMap = A.isNotEmpty(currentStyleMap) ? currentStyleMap : new HashMap<>();

        CellStyle cellStyle = tmpStyleMap.get(style);
        if (U.isNotBlank(cellStyle)) {
            return cellStyle;
        } else {
            // 将 "数字格式(比如金额用 0.00)~r~b" 转换成具体的样式
            CellStyle tmpStyle = isNumber ? numberStyle(workbook) : contentStyle(workbook);
            if (U.isNotBlank(style)) {
                String[] format = style.split("~");
                tmpStyle.setDataFormat(dataFormat.getFormat(format[0].trim()));

                if (format.length > 1) {
                    String lrc = format[1].trim();
                    if (U.isNotBlank(lrc)) {
                        Map<String, HorizontalAlignment> tmpMap = A.maps(
                                "r", HorizontalAlignment.RIGHT,
                                "l", HorizontalAlignment.LEFT,
                                "c", HorizontalAlignment.CENTER
                        );
                        HorizontalAlignment alignment = tmpMap.get(lrc.toLowerCase());
                        if (U.isNotBlank(alignment)) {
                            tmpStyle.setAlignment(alignment);
                        }
                    }
                }

                if (format.length > 2) {
                    String f = format[2].trim();
                    if (U.isNotBlank(f)) {
                        Font font = workbook.createFont();
                        if ("b".equalsIgnoreCase(f)) {
                            font.setBold(true);
                        } else if ("i".equalsIgnoreCase(f)) {
                            font.setItalic(true);
                        } else if ("s".equalsIgnoreCase(f)) {
                            font.setStrikeout(true);
                        }
                        font.setFontHeightInPoints(FONT_SIZE);
                        tmpStyle.setFont(font);
                    }
                }
                tmpStyleMap.put(style, tmpStyle);
                CUSTOMIZE_CELL_STYLE.set(tmpStyleMap);
            }
            return tmpStyle;
        }
    }
}
