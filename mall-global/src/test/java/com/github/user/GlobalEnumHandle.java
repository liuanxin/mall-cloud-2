package com.github.user;

import com.github.common.Const;
import com.github.common.util.GenerateEnumHandler;
import com.github.global.constant.GlobalConst;
import org.junit.Test;

public class GlobalEnumHandle {

    @Test
    public void generateEnum() {
        GenerateEnumHandler.generateEnum(getClass(), Const.BASE_PACKAGE, GlobalConst.MODULE_NAME);
    }
}
