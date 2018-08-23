package com.github.common.service;

import com.github.common.config.Url;
import com.github.common.json.JsonResult;
import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.LogUtil;
import com.netflix.discovery.converters.Auto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共模块的接口实现类
 *
 * @author https://github.com/liuanxin
 */
@RestController
public class CommonServiceImpl implements CommonInterface {

    @Autowired
    private Url url;
    
    @Override
    public PageInfo demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用实现类" + xx + ", page:" + page + ", limit:" + limit);
        }
        return Pages.returnPage(null);
    }

    @GetMapping("/")
    public JsonResult index() {
        System.out.println(url);
        return JsonResult.success("Common-module");
    }
}
