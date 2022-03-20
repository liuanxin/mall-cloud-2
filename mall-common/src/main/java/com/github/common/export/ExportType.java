package com.github.common.export;

import com.github.common.util.U;

/** 如果想要将数据导成文件保持, 使用 {@link FileExport} 类, 如果要导出文件在 web 端下载, 使用 {@link WebExport} 类 */
public enum ExportType {

    XLS, XLSX, CSV;

    public boolean is03() {
        return this == XLS;
    }
    public boolean is07() {
        return this == XLSX;
    }
    public boolean isExcel() {
        return is03() || is07();
    }

    public boolean isCsv() {
        return this == CSV;
    }

    public static ExportType to(String type) {
        if (U.isNotNull(type)) {
            for (ExportType exportType : values()) {
                if (type.equalsIgnoreCase(exportType.name())) {
                    return exportType;
                }
            }
        }
        return XLSX;
    }
}
