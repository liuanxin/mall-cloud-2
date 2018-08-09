package com.github.user;

import com.github.common.Const;
import com.github.common.util.GenerateEnumHandler;
import com.github.user.constant.UserConst;
import org.junit.Test;

/**
 * 用户模块生成 enumHandle 的工具类
 *
 * @author https://github.com/liuanxin
 */
public class UserGenerateEnumHandler {

    @Test
    public void generate() {
        GenerateEnumHandler.generateEnum(getClass(), Const.BASE_PACKAGE, UserConst.MODULE_NAME);
    }
}
