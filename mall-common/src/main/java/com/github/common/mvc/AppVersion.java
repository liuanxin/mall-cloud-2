package com.github.common.mvc;

/** 版本 */
public enum AppVersion {

    // !!! 请保证大版本的 code 比小版本的 code 大 !!!

    V100(100, "1.0.0"),

    V101(101, "1.0.1"),

    V102(102, "1.0.2"),

    V103(103, "1.0.3"),

    V104(104, "1.0.4");

    int code;
    String value;
    AppVersion(int code, String value) {
        this.code = code;
        this.value = value;
    }
    public int getCode() {
        return code;
    }
    public String getValue() {
        return value;
    }

    /**
     * 如果请求的是 v3, 标注的有 v v1 v2 和 v5 四个方法, 则 v1 和 v2 会返回, 而 v5 则不会, v 不会参与对比
     *
     * @param version 标注在 web 端方法上的版本信息
     * @return 如果从前端来的版本信息比方法上的版本信息要高则返回 true
     */
    public boolean greaterOrEqual(AppVersion version) {
        return (version != null) && (code >= version.code);
    }

    public static String currentVersion() {
        return V100.value;
    }
}
