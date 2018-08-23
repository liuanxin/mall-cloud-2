package com.github.queue.client;

import com.github.queue.service.QueueInterface;
import com.github.queue.constant.QueueConst;
import com.github.queue.hystrix.QueueFallback;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 消息队列相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = QueueConst.MODULE_NAME, fallback = QueueFallback.class)
public interface QueueService extends QueueInterface {
}
