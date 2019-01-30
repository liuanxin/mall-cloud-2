package com.github.search.hystrix;

import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.LogUtil;
import com.github.search.client.SearchClient;
import org.springframework.stereotype.Component;

/**
 * 搜索相关的断路器
 */
@Component
public class SearchClientFallback implements SearchClient {

    @Override
    public PageInfo demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return Pages.returnPage(null);
    }
}
