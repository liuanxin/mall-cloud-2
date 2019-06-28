package com.github.common.export;

import com.github.common.util.A;
import com.github.common.util.U;

import java.util.LinkedHashMap;
import java.util.List;

/** 如果想要将数据导成文件保持, 使用 {@link FileExport} 类, 如果要导出文件在 web 端下载, 使用 {@link WebExport} 类 */
final class ExportCsv {

    static String getContent(LinkedHashMap<String, String> titleMap, List<?> dataList) {
        if (A.isNotEmpty(titleMap) && A.isNotEmpty(dataList)) {
            // csv 用英文逗号(,)隔开列, 用换行(\n)隔开行, 内容中包含了逗号的需要用双引号包裹, 若内容中包含了双引号则需要用两个双引号表示.
            StringBuilder sbd = new StringBuilder();
            int i = 0;
            for (String title : titleMap.values()) {
                sbd.append(handleCsvContent(title.split("\\|")[0]));
                i++;
                if (i != titleMap.size()) {
                    sbd.append(",");
                }
            }
            if (A.isNotEmpty(dataList)) {
                for (Object data : dataList) {
                    if (sbd.length() > 0) {
                        sbd.append("\n");
                    }
                    i = 0;
                    for (String title : titleMap.keySet()) {
                        sbd.append(handleCsvContent(U.getField(data, title)));
                        i++;
                        if (i != titleMap.size()) {
                            sbd.append(",");
                        }
                    }
                }
            }
            return sbd.toString();
        }
        // 没有数据或没有标题, 返回一个内容为空的文件
        return U.EMPTY;
    }

    private static String handleCsvContent(String content) {
        return "\"" + content.replace("\"", "\"\"") + "\"";
    }
}
