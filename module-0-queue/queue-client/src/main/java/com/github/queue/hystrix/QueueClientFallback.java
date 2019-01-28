package com.github.queue.hystrix;

import com.github.common.util.LogUtil;
import com.github.queue.client.QueueClient;
import org.springframework.stereotype.Component;

/**
 * 消息队列相关的断路器
 *
 * @author https://github.com/liuanxin
 */
@Component
public class QueueClientFallback implements QueueClient {

    @Override
    public void submitSimple(String simpleInfo) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
    }
}
