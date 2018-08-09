package com.github.queue.constant;

/**
 * 消息队列模块相关的常数设置类
 *
 * @author https://github.com/liuanxin
 */
public final class QueueConst {

    /** 当前模块名. 要与 bootstrap.yml 中的一致 */
    public static final String MODULE_NAME = "queue";

    /** 当前模块说明. 当用在文档中时有用 */
    public static final String MODULE_INFO = MODULE_NAME + "-消息队列";


    // ========== 用到的消息队列名 ==========

    public static final String SIMPLE_MQ_NAME = "simple-mq";


    // ========== url 说明 ==========

    /** 测试地址 */
    public static final String QUEUE_DEMO = MODULE_NAME + "/demo";
}
