package com.github.common.export.poi;

import com.github.common.util.A;
import com.github.common.util.U;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 如果想要将数据导成文件保持, 使用 {@link com.github.common.export.FileExport} 类
 * 如果要导出文件在 web 端下载, 使用 {@link com.github.common.export.WebExport} 类
 */
@SuppressWarnings("DuplicatedCode")
public class ExportExcel {

    /** 标题行字体大小 */
    private static final short TITLE_FONT_SIZE = 14;
    /** 行字体大小 */
    private static final short FONT_SIZE = 12;

    private static int getMaxColumn(boolean excel07) {
        return excel07 ? 16384 : 256;
    }
    // 单 sheet:
    //   2003(xls)       最多只能有    65,536 行     256 列
    //   2007(xlsx) 及以上最多只能有 1,048,576 行 16,384 列
    private static int getMaxRow(boolean excel07) {
        // return excel07 ? 1048576 : 65535;
        return excel07 ? 300_000 : 50_000;
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
     *   "商品信息": { "title": "商品标题", "image": "图片" }
     * }
     *
     * dataMap: key 是 sheet name, value 是这个 sheet 的数据(实体 或 Map 都可), 单条数据的字段名 跟 上面的标题映射 一一对应, 如
     * {
     *   "用户信息": [ { "name": "张三", "desc": "三儿" }, { "name": "李四", "desc": "四儿" } ... ],
     *   "商品信息": [ { "title": "苹果", "image": "xxx" }, { "title": "三星", "image": "yyy" } ]
     * }
     *
     * 最终导出时大概是这样(假定用户信息的数据很多, 此时会有三个 sheet, 前两个是用户信息, 后一个是商品信息)
     *
     * | 用户    说明    |    用户    说明    |    商品标题    图片 |
     * | 张三    三儿    |    王五    五儿    |    苹果       xxx  |
     * | 李四    四儿    |    钱六    六儿    |    三星       yyy  |
     * |                |                   |                   |
     * | 用户信息-1      |    用户信息-2      |    商品信息        |
     *
     *
     * 使用 response 直接导出:
     * try ( Workbook workbook = ExportExcel.handle( ... ) ) {
     *     try {
     *         workbook.write(response.getOutputStream());
     *     } finally {
     *         ExportExcel.dispose(workbook);
     *     }
     * } catch (Exception e) {
     *     // 导出失败...
     * }
     *
     * 使用字节流:
     * try (
     *         ByteArrayOutputStream out = new ByteArrayOutputStream();
     *         Workbook workbook = ExportExcel.handle( ... )
     * ) {
     *     try {
     *         workbook.write(out);
     *         byte[] bytes = out.toByteArray();
     *         // 操作字节流, 比如上传到 oss 等...
     *     } finally {
     *         ExportExcel.dispose(workbook);
     *     }
     * } catch (Exception e) {
     *     // 导出失败...
     * }
     * </pre>
     */
    public static Workbook handle(boolean excel07, Map<String, LinkedHashMap<String, String>> titleMap,
                                  LinkedHashMap<String, List<?>> dataMap) {
        Workbook workbook = create(excel07);
        // 没有标题直接返回
        if (A.isEmpty(titleMap)) {
            return workbook;
        }

        int maxColumn = getMaxColumn(excel07);
        int columnSize = titleMap.size();
        if (columnSize > maxColumn) {
            throw new RuntimeException("Invalid column number " + columnSize + ", max " + maxColumn);
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

        Sheet sheet;
        //  行
        Row row;
        //   列
        Cell cell;
        //  表格数       行索引     列索引
        int sheetCount, rowIndex, cellIndex;
        //  数据总条数  数据起始索引  数据结束索引
        int size, fromIndex, toIndex;
        //      数据
        List<?> dataList;

        // 单个列的数据
        String cellData;

        // 每个 sheet 的最大行, 标题头也是一行
        int sheetMaxRow = getMaxRow(excel07) - 1;
        for (Map.Entry<String, List<?>> entry : dataMap.entrySet()) {
            String sheetName = entry.getKey();
            // 标题头, 这里跟数据中的属性相对应
            Set<Map.Entry<String, String>> titleEntry = titleMap.get(sheetName).entrySet();

            // 当前 sheet 的数据
            dataList = entry.getValue();
            size = A.isEmpty(dataList) ? 0 : dataList.size();
            boolean hasBigData = size > sheetMaxRow;

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
                for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                    // 标题列
                    cell = row.createCell(cellIndex);
                    cell.setCellStyle(headStyle);

                    String title = U.toStr(titleMapEntry.getValue().split("\\|")[0]);
                    cell.setCellValue(title);
                    setWidthAndHeight(cell, title);
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
                            for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                                // 数据列
                                cell = row.createCell(cellIndex);

                                cellData = U.getFieldMethod(data, titleMapEntry.getKey());
                                if (U.isNumber(cellData)) {
                                    if (!hasBigData) {
                                        cell.setCellStyle(numberStyle);
                                    }
                                    cell.setCellValue(U.toDouble(cellData));
                                } else {
                                    if (!hasBigData) {
                                        cell.setCellStyle(contentStyle);
                                    }
                                    cell.setCellValue(cellData);
                                }
                                setWidthAndHeight(cell, cellData);
                                cellIndex++;
                            }
                        }
                    }
                }
            }
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
    /**
     * <pre>
     * 清除临时文件
     *
     * 使用 response 直接导出:
     * try ( Workbook workbook = ExportExcel.handle( ... ) ) {
     *     try {
     *         workbook.write(response.getOutputStream());
     *     } finally {
     *         ExportExcel.dispose(workbook);
     *     }
     * }
     *
     * 使用字节流:
     * try (
     *         ByteArrayOutputStream out = new ByteArrayOutputStream();
     *         Workbook workbook = ExportExcel.handle( ... )
     * ) {
     *     try {
     *         workbook.write(out);
     *         byte[] bytes = out.toByteArray();
     *         // 操作字节流, 比如上传到 oss 等...
     *     } finally {
     *         ExportExcel.dispose(workbook);
     *     }
     * }
     * </pre>
     */
    public static void dispose(Workbook workbook) {
        if (U.isNotNull(workbook) && workbook instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook) workbook).dispose();
        }
    }

    /**
     * 将特殊字符替换成空格, 最开始及最结尾是单引号则去掉, 多个空格替换成一个, 如果有多个 sheet 就拼在名字后面, 长度超过 31 则截取
     *
     * @see org.apache.poi.ss.util.WorkbookUtil#validateSheetName
     */
    private static String handleSheetName(String sheetName, int sheetCount, int sheetIndex) {
        if (U.isNull(sheetName)) {
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
        String tmp = tmpSn.replaceAll("\\s{2,}", " ");
        String indexSuffix = (sheetCount > 1) ? (" - " + (sheetIndex + 1)) : U.EMPTY;
        int nameLen = 31 - indexSuffix.length();
        return ((tmp.length() > nameLen) ? tmp.substring(0, nameLen) : tmp) + indexSuffix;
    }

    /** 设置列的宽和高 */
    private static void setWidthAndHeight(Cell cell, String data) {
        if (data != null && data.trim().length() > 0) {
            // 内容里面有换行则宽度以最长的行为主, 行高以行数为主
            int maxWidth = 0;
            String[] lines = data.split("\n");
            for (String s : lines) {
                // 列宽: 从内容来确定, 中文为 2 个长度, 左移 8 相当于 * 256
                int lineWidth = (U.toLen(s) + 2) << 8;
                if (lineWidth > maxWidth) {
                    maxWidth = lineWidth;
                }
            }
            int lineNum = lines.length;

            Sheet sheet = cell.getSheet();
            int columnIndex = cell.getColumnIndex();
            int currentWidth = sheet.getColumnWidth(columnIndex);
            if (maxWidth > currentWidth) {
                sheet.setColumnWidth(columnIndex, maxWidth);
            }

            if (lineNum > 1) {
                // 设置为内容换行, 不然显示依然是在一行的, 需要在内容上编辑一下才会显示换行效果
                cell.getCellStyle().setWrapText(true);
                Row row = cell.getRow();
                row.setHeightInPoints((row.getHeightInPoints() + 2) * lineNum);
            }
        }
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
}
