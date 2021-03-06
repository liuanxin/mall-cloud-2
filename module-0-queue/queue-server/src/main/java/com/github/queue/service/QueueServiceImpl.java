package com.github.queue.service;

import com.github.common.json.JsonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息队列模块的接口实现类
 */
@RestController
public class QueueServiceImpl implements QueueService {
    
    @Override
    public void submitSimple(String simpleInfo) {
    }

    @GetMapping("/")
    public JsonResult index() {
        return JsonResult.success("Queue-module");
    }
}
