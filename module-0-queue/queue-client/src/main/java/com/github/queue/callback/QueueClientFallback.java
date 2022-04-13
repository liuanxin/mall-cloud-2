package com.github.queue.callback;

import com.github.common.util.LogUtil;
import com.github.queue.client.QueueClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 消息队列相关的断路器
 */
@Component
public class QueueClientFallback implements FallbackFactory<QueueClient> {

    @Override
    public QueueClient create(Throwable e) {
        return new QueueClient() {
            @Override
            public void submitSimple(String simpleInfo) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("熔断了", e);
                }
            }
        };
    }
}
