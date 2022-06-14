package com.github.common.export.csv;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ExportCsv {

    // 见: https://zh.wikipedia.org/wiki/%E9%80%97%E5%8F%B7%E5%88%86%E9%9A%94%E5%80%BC
    // 用英文逗号(,)隔开列, 用换行(\n)隔开行
    //   内容中包含了双引号则替换成两个双引号, 内容中包含了逗号、空格或双引号的则用双引号包裹
    //   为避免处理换行时出问题: 内容中包含了换行的替换成空格

    private static final String SPLIT = ",";
    private static final String WIN_WRAP = "\r";
    private static final String WRAP = "\n";
    private static final String BLANK = "";
    private static final String SPACE = " ";
    private static final String QUOTE = "\"";
    private static final String REPLACE_QUOTE = "\"\"";

    public static String getContent(LinkedHashMap<String, String> titleMap, List<?> dataList) {
        StringBuilder sbd = new StringBuilder();
        if (A.isNotEmpty(titleMap)) {
            int i = 0;
            for (String title : titleMap.values()) {
                sbd.append(handleCsvContent(title));
                i++;
                if (i != titleMap.size()) {
                    sbd.append(SPLIT);
                }
            }
            if (A.isNotEmpty(dataList)) {
                Set<String> titles = titleMap.keySet();
                for (Object data : dataList) {
                    if (sbd.length() > 0) {
                        sbd.append(WRAP);
                    }
                    i = 0;
                    for (String title : titles) {
                        sbd.append(handleCsvContent(U.getFieldMethod(data, title)));
                        i++;
                        if (i != titleMap.size()) {
                            sbd.append(SPLIT);
                        }
                    }
                }
            }
        }
        return sbd.toString();
    }

    public static String writeCsvHead(LinkedHashMap<String, String> titleMap) {
        StringBuilder sbd = new StringBuilder();
        if (titleMap != null && titleMap.size() > 0) {
            int i = 0;
            for (String title : titleMap.values()) {
                sbd.append(handleCsvContent(title));
                i++;
                if (i != titleMap.size()) {
                    sbd.append(SPLIT);
                }
            }
        }
        return sbd.toString();
    }

    public static String writeCsvContent(LinkedHashSet<String> titles, List<?> dataList) {
        StringBuilder sbd = new StringBuilder();
        if (titles != null && titles.size() > 0 && dataList != null && dataList.size() > 0) {
            for (Object data : dataList) {
                sbd.append(WRAP);

                int i = 0;
                for (String title : titles) {
                    sbd.append(handleCsvContent(U.getFieldMethod(data, title)));
                    i++;
                    if (i != titles.size()) {
                        sbd.append(SPLIT);
                    }
                }
            }
        }
        return sbd.toString();
    }

    private static String handleCsvContent(String content) {
        if (U.isNull(content)) {
            return U.EMPTY;
        }
        if (U.isNotBlank(content)) {
            if (content.contains(WIN_WRAP)) {
                content = content.replace(WIN_WRAP, BLANK);
            }
            if (content.contains(WRAP)) {
                content = content.replace(WRAP, SPACE);
            }
            if (content.contains(QUOTE)) {
                content = content.replace(QUOTE, REPLACE_QUOTE);
            }
            if (content.contains(SPLIT) || content.contains(SPACE) || content.contains(QUOTE)) {
                content = QUOTE + content + QUOTE;
            }
        }
        return content;
    }
}
