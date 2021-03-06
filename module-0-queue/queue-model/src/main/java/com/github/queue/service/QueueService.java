package com.github.queue.service;

import com.github.queue.constant.QueueConst;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 消息队列相关的接口
 */
public interface QueueService {

    @GetMapping(QueueConst.QUEUE_DEMO)
    void submitSimple(String simpleInfo);
}
