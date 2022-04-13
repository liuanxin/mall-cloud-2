package com.github.user.callback;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.user.client.UserClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 用户相关的断路器
 */
@Component
public class UserClientFallback implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable e) {
        return new UserClient() {
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
