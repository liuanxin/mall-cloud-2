package com.github.search.callback;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.search.client.SearchClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 搜索相关的断路器
 */
@Component
public class SearchClientFallback implements FallbackFactory<SearchClient> {

    @Override
    public SearchClient create(Throwable e) {
        return new SearchClient() {
            @Override
            public PageReturn demo(String xx, Integer page, Integer limit) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("熔断了", e);
                }
                return null;
            }
        };
    }
}
