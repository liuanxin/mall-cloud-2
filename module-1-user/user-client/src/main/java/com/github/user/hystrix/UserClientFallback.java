package com.github.user.hystrix;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.user.client.UserClient;
import org.springframework.stereotype.Component;

/**
 * 用户相关的断路器
 */
@Component
public class UserClientFallback implements UserClient {

    @Override
    public PageReturn demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return null;
    }
}
