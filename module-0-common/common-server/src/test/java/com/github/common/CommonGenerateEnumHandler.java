package com.github.common;

import com.github.common.constant.CommonConst;
import com.github.common.util.GenerateEnumHandler;
import org.junit.Test;

/**
 * 公共模块生成 enumHandle 的工具类
 */
public class CommonGenerateEnumHandler {

    @Test
    public void generate() {
        GenerateEnumHandler.generateEnum(getClass(), Const.BASE_PACKAGE, CommonConst.MODULE_NAME);
    }
}
