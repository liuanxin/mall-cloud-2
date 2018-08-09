package com.github.queue;

import com.github.common.Const;
import com.github.common.util.GenerateEnumHandler;
import com.github.queue.constant.QueueConst;
import org.junit.Test;

/**
 * 消息队列模块生成 enumHandle 的工具类
 *
 * @author https://github.com/liuanxin
 */
public class QueueGenerateEnumHandler {

    @Test
    public void generate() {
        GenerateEnumHandler.generateEnum(getClass(), Const.BASE_PACKAGE, QueueConst.MODULE_NAME);
    }
}
