package com.github.common.service;

import com.github.common.json.JsonResult;
import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共模块的接口实现类
 */
@RestController
public class CommonServiceImpl implements CommonService {

    @Override
    public PageReturn demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用实现类" + xx + ", page:" + page + ", limit:" + limit);
        }
        return null;
    }

    @GetMapping("/")
    public JsonResult index() {
        return JsonResult.success("Common-module");
    }
}
