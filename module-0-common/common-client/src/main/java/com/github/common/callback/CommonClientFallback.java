package com.github.common.callback;

import com.github.common.client.CommonClient;
import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 公共相关的断路器
 */
@Component
public class CommonClientFallback implements FallbackFactory<CommonClient> {

    @Override
    public CommonClient create(Throwable e) {
        return new CommonClient() {
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
