package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.U;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 如果想要将数据导成文件保持, 使用 {@link FileExport} 类, 如果要导出文件在 web 端下载, 使用 {@link WebExport} 类 */
final class ExportExcel {

    // excel 2003 的最大行数是 65536 行, 2007 开始的版本是 1048576 行.
    // excel 2003 的最大列数是 256 列, 2007 及以上版本是 16384 列.
    /** excel 单个 sheet 能处理的最大行数 (2 << 15) - 1 */
    private static final int EXCEL_TOTAL = 65535;

    /** 标题行的字体大小 */
    private static final short HEAD_FONT_SIZE = 11;

    /** 其他内容的字体大小 */
    private static final short FONT_SIZE = 10;

    /** 行高. 要比上面的字体大一点! */
    private static final short ROW_HEIGHT = 15;

    /**
     * 返回一个 excel 工作簿
     *
     * @param excel07  是否返回 microsoft excel 2007 的版本
     * @param titleMap 属性名为 key, 对应的标题为 value, 为了处理显示时的顺序, 因此使用 linkedHashMap
     * @param dataMap  以「sheet 名」为 key, 对应的数据为 value(每一行的数据为一个 Object)
     */
    static Workbook handle(boolean excel07, LinkedHashMap<String, String> titleMap,
                           LinkedHashMap<String, List<?>> dataMap) {
        // 声明一个工作薄. HSSFWorkbook 是 Office 2003 的版本, XSSFWorkbook 是 2007
        Workbook workbook = excel07 ? new XSSFWorkbook() : new HSSFWorkbook();
        // 没有标题直接返回
        if (A.isEmpty(titleMap)) {
            return workbook;
        }
        // 如果数据为空, 构建一个空字典(确保导出的文件有标题头)
        if (dataMap == null) {
            dataMap = new LinkedHashMap<>();
        }

        // 头样式
        CellStyle headStyle = createHeadStyle(workbook);
        // 内容样式
        CellStyle contentStyle = createContentStyle(workbook);
        // 数字样式
        CellStyle numberStyle = createNumberStyle(workbook);
        // 每个列用到的样式
        CellStyle cellTmpStyle;

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
        //      每个 sheet 的数据, 当数据量大, 有多个 sheet 时会用到
        List<?> sheetList;

        // 标题头, 这里跟数据中的属性相对应
        Set<Map.Entry<String, String>> titleEntry = titleMap.entrySet();
        // 是否能正确中文字体的大小
        boolean rightChineseFont = false;

        // 单个列的数据
        String cellData;
        // 标题说明|数字格式(比如金额用 0.00)|宽度(255 以内)
        String[] titleValues;
        // 数字格式
        DataFormat dataFormat = workbook.createDataFormat();

        for (Map.Entry<String, List<?>> entry : dataMap.entrySet()) {
            // 当前 sheet 的数据
            dataList = entry.getValue();
            size = A.isEmpty(dataList) ? 0 : dataList.size();
            // 一个 sheet 数据过多 excel 处理会出错, 分多个 sheet
            sheetCount = ((size % EXCEL_TOTAL == 0) ? (size / EXCEL_TOTAL) : (size / EXCEL_TOTAL + 1));
            if (sheetCount == 0) {
                // 如果没有记录时也至少构建一个(确保导出的文件有标题头)
                sheetCount = 1;
            }
            for (int i = 0; i < sheetCount; i++) {
                // 构建 sheet, 带名字
                sheet = workbook.createSheet(entry.getKey() + (sheetCount > 1 ? ("-" + (i + 1)) : U.EMPTY));

                // 每个 sheet 的标题行
                rowIndex = 0;
                cellIndex = 0;
                row = sheet.createRow(rowIndex);
                row.setHeightInPoints(ROW_HEIGHT);
                // 每个 sheet 的标题行
                for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                    cell = row.createCell(cellIndex);
                    cell.setCellStyle(headStyle);
                    cell.setCellValue(U.getNil(titleMapEntry.getValue().split("\\|")[0]));
                    cellIndex++;
                }
                // 冻结第一行
                sheet.createFreezePane(0, 1, 0, 1);

                if (size > 0) {
                    if (sheetCount > 1) {
                        // 每个 sheet 除标题行以外的数据
                        fromIndex = EXCEL_TOTAL * i;
                        toIndex = (i + 1 == sheetCount) ? size : EXCEL_TOTAL;
                        sheetList = dataList.subList(fromIndex, toIndex);
                    } else {
                        sheetList = dataList;
                    }
                    for (Object data : sheetList) {
                        if (data != null) {
                            rowIndex++;
                            // 每行
                            row = sheet.createRow(rowIndex);
                            row.setHeightInPoints(ROW_HEIGHT);

                            cellIndex = 0;
                            for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                                // 每列
                                cell = row.createCell(cellIndex);

                                cellData = U.getField(data, titleMapEntry.getKey());
                                if (NumberUtils.isCreatable(cellData)) {
                                    cell.setCellType(CellType.NUMERIC);
                                    cell.setCellValue(NumberUtils.toDouble(cellData));

                                    titleValues = titleMapEntry.getValue().split("\\|");
                                    if (titleValues.length > 1) {
                                        // 数字样式需要单独设置格式, 每次都生成一个
                                        cellTmpStyle = createNumberStyle(workbook);
                                        cellTmpStyle.setDataFormat(dataFormat.getFormat(titleValues[1]));
                                    } else {
                                        cellTmpStyle = numberStyle;
                                    }
                                } else {
                                    cell.setCellType(CellType.STRING);
                                    cell.setCellValue(cellData);
                                    // 给字符串设置样式意义并不大, 忽略样式. 此处并不每次都生成一个
                                    cellTmpStyle = contentStyle;
                                }
                                cell.setCellStyle(cellTmpStyle);
                                cellIndex++;
                            }
                        }
                    }
                }

                cellIndex = 0;
                for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                    // 让列的宽度自适应. 缺少中文字体计算宽度时会有问题, 可以统一宽度并接受自定义列宽度(像下面), 或者复制字体文件到操作系统
                    if (rightChineseFont) {
                        sheet.autoSizeColumn(cellIndex, true);
                    } else {
                        // 左移 8 相当于 * 256
                        sheet.setColumnWidth(cellIndex, 12 << 8);

                        titleValues = titleMapEntry.getValue().split("\\|");
                        if (titleValues.length > 2) {
                            int width = NumberUtils.toInt(titleValues[2]);
                            if (width > 0) {
                                // 左移 8 相当于 * 256
                                sheet.setColumnWidth(cellIndex, width << 8);
                            }
                        }
                    }
                    cellIndex++;
                }
            }
        }
        return workbook;
    }

    /** 头样式 */
    private static CellStyle createHeadStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // 水平居左
        style.setAlignment(HorizontalAlignment.LEFT);
        // 垂直居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        // 粗体
        font.setBold(true);
        font.setFontHeightInPoints(HEAD_FONT_SIZE);
        style.setFont(font);
        return style;
    }

    /** 内容样式 */
    private static CellStyle createContentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);
        return style;
    }

    /** 数字样式 */
    private static CellStyle createNumberStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);
        return style;
    }
}
