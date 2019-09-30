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

    /** 标题行的字体大小 */
    private static final short HEAD_FONT_SIZE = 11;
    /** 其他内容的字体大小 */
    private static final short FONT_SIZE = 10;
    /** 行高. 要比上面的字体大一点! */
    private static final short ROW_HEIGHT = 15;

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


        // 单个列的数据
        String cellData;
        // 标题说明|数字格式(比如金额用 0.00)|宽度(255 以内)
        String[] titleValues;
        // 数字格式
        DataFormat dataFormat = workbook.createDataFormat();

        // 每个 sheet 的最大行, 标题头也是一行
        int maxRow = getMaxRow(excel07) - 1;
        for (Map.Entry<String, List<?>> entry : dataMap.entrySet()) {
            String sheetName = entry.getKey();
            // 标题头, 这里跟数据中的属性相对应
            Set<Map.Entry<String, String>> titleEntry = titleMap.get(sheetName).entrySet();

            // 当前 sheet 的数据
            dataList = entry.getValue();
            size = A.isEmpty(dataList) ? 0 : dataList.size();

            // 一个 sheet 数据过多 excel 处理会出错, 分多个 sheet
            sheetCount = ((size % maxRow == 0) ? (size / maxRow) : (size / maxRow + 1));
            if (sheetCount == 0) {
                // 如果没有记录时也至少构建一个(确保导出的文件有标题头)
                sheetCount = 1;
            }

            for (int i = 0; i < sheetCount; i++) {
                // 构建 sheet, 带名字
                sheet = workbook.createSheet(sheetName + (sheetCount > 1 ? ("-" + (i + 1)) : U.EMPTY));

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
                        fromIndex = maxRow * i;
                        toIndex = (i + 1 == sheetCount) ? size : maxRow;
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
                                titleValues = titleMapEntry.getValue().split("\\|");

                                boolean isNumber = NumberUtils.isCreatable(cellData);
                                if (titleValues.length > 1) {
                                    // 自定义格式
                                    cellTmpStyle = createNumberStyle(workbook);
                                    cellTmpStyle.setDataFormat(dataFormat.getFormat(titleValues[1]));
                                } else {
                                    cellTmpStyle = isNumber ? numberStyle : contentStyle;
                                }
                                if (isNumber) {
                                    cell.setCellValue(NumberUtils.toDouble(cellData));
                                } else {
                                    cell.setCellValue(cellData);
                                }

                                cell.setCellStyle(cellTmpStyle);
                                cellIndex++;
                            }
                        }
                    }
                }

                cellIndex = 0;
                for (Map.Entry<String, String> titleMapEntry : titleEntry) {
                    titleValues = titleMapEntry.getValue().split("\\|");
                    if (titleValues.length > 2) {
                        int width = U.toInt(titleValues[2]);
                        if (width > 0 && width < 256) {
                            // 左移 8 相当于 * 256
                            sheet.setColumnWidth(cellIndex, width << 8);
                        } else {
                            // 左移 8 相当于 * 256
                            sheet.setColumnWidth(cellIndex, 12 << 8);
                        }
                    } else {
                        // 让列的宽度自适应. 缺少中文字体计算宽度时会有问题, 需要复制中文字体文件到操作系统
                        sheet.autoSizeColumn(cellIndex, true);
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
