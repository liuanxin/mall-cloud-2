package com.github.queue.service;

import com.github.common.json.JsonResult;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.LogUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息队列模块的接口实现类
 *
 * @author https://github.com/liuanxin
 */
@RestController
public class QueueServiceImpl implements QueueInterface {
    
    @Override
    public void submitSimple(String simpleInfo) {
    }

    @GetMapping("/")
    public JsonResult index() {
        return JsonResult.success("Queue-module");
    }
}
