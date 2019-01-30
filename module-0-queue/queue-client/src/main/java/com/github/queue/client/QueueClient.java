package com.github.queue.client;

import com.github.queue.constant.QueueConst;
import com.github.queue.hystrix.QueueClientFallback;
import com.github.queue.service.QueueService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 消息队列相关的调用接口
 */
@FeignClient(value = QueueConst.MODULE_NAME, fallback = QueueClientFallback.class)
public interface QueueClient extends QueueService {
}
