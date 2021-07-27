package com.github.common.hystrix;

import com.github.common.client.CommonClient;
import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import org.springframework.stereotype.Component;

/**
 * 公共相关的断路器
 */
@Component
public class CommonClientFallback implements CommonClient {

    @Override
    public PageReturn demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return null;
    }
}
