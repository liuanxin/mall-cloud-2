package com.github.common.export.easy;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.github.common.util.A;
import com.github.common.util.U;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
@AllArgsConstructor
public class StyleCellHandler implements CellWriteHandler {

    /** 标题行字体大小 */
    private static final short TITLE_FONT_SIZE = 14;
    /** 行字体大小 */
    private static final short FONT_SIZE = 12;

    private static final int[] TITLE_RGB = new int[] { 160, 200, 230 };

    private static final Cache<Thread, Map<String, CellStyle>> STYLE_CACHE =
            CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    private final boolean hasCellStyle;


    @Override
    public void beforeCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                 Row row, Head head, Integer columnIndex, Integer relativeRowIndex, Boolean isHead) {
    }

    @Override
    public void afterCellCreate(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        if (isHead != null) {
            if (isHead) {
                cell.setCellStyle(headStyle(cell.getSheet().getWorkbook()));
                setWidthAndHeight(cell, head.getHeadNameList().get(relativeRowIndex));
            }
        }
    }

    private void setWidthAndHeight(Cell cell, String data) {
        if (U.isNotNull(data)) {
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
                row.setHeightInPoints(row.getHeightInPoints() * lineNum);
            }
        }
    }

    @Override
    public void afterCellDataConverted(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                       WriteCellData<?> cellData, Cell cell, Head head,
                                       Integer relativeRowIndex, Boolean isHead) {
        if (hasCellStyle) {
            return;
        }
        if (isHead != null) {
            // convert 后 head 走不到这里来, 只在 afterCellCreate 处执行了
            /* if (isHead) {
                cell.setCellStyle(headStyle(cell.getSheet().getWorkbook()));
                settingWidth(cell, head.getHeadNameList().get(relativeRowIndex));
            } else { */
            if (!isHead) {
                if (U.isNotNull(cellData)) {
                    CellDataTypeEnum dataType = cellData.getType();
                    if (dataType == CellDataTypeEnum.NUMBER || dataType == CellDataTypeEnum.BOOLEAN) {
                        cell.setCellStyle(numberStyle(cell.getSheet().getWorkbook()));
                    } else if (dataType == CellDataTypeEnum.STRING || dataType == CellDataTypeEnum.DIRECT_STRING) {
                        cell.setCellStyle(contentStyle(cell.getSheet().getWorkbook()));
                    }
                    setWidthAndHeight(cell, cellData.toString());
                }
            }
        }
    }


    /** 头样式: 垂直居中, 水平居左, 粗体, 字体大小 */
    private static CellStyle headStyle(Workbook workbook) {
        String key = "head";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = new ConcurrentHashMap<>();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);

        // 设置标题行背景
        int r = TITLE_RGB[0], g = TITLE_RGB[1], b = TITLE_RGB[2];
        if (style instanceof XSSFCellStyle) {
            IndexedColorMap colorMap = new DefaultIndexedColorMap();
            ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(new java.awt.Color(r, g, b), colorMap));
        } else if (workbook instanceof HSSFWorkbook) {
            HSSFPalette palette = ((HSSFWorkbook) workbook).getCustomPalette();
            HSSFColor color = palette.findSimilarColor(r, g, b);
            if (color == null) {
                short replaceIndex = IndexedColors.LAVENDER.getIndex();
                palette.setColorAtIndex(replaceIndex, (byte) r, (byte) g, (byte) b);
                color = palette.getColor(replaceIndex);
            }
            style.setFillForegroundColor(color.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }

    /** 内容样式: 垂直居中, 水平居左, 字体大小 */
    private static CellStyle contentStyle(Workbook workbook) {
        String key = "content";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = new ConcurrentHashMap<>();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }

    /** 数字样式: 垂直居中, 水平居右, 字体大小 10 */
    private static CellStyle numberStyle(Workbook workbook) {
        String key = "number";
        Thread currentThread = Thread.currentThread();
        Map<String, CellStyle> styleMap = STYLE_CACHE.getIfPresent(currentThread);
        if (A.isEmpty(styleMap)) {
            styleMap = new ConcurrentHashMap<>();
        } else if (styleMap.containsKey(key)) {
            return styleMap.get(key);
        }

        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.RIGHT);

        Font font = workbook.createFont();
        font.setFontHeightInPoints(FONT_SIZE);
        style.setFont(font);

        styleMap.put(key, style);
        STYLE_CACHE.put(currentThread, styleMap);
        return style;
    }
}
