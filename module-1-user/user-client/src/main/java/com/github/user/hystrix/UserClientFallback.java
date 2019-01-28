package com.github.user.hystrix;

import com.github.common.page.PageInfo;
import com.github.common.page.Pages;
import com.github.common.util.LogUtil;
import com.github.user.client.UserClient;
import org.springframework.stereotype.Component;

/**
 * 用户相关的断路器
 *
 * @author https://github.com/liuanxin
 */
@Component
public class UserClientFallback implements UserClient {

    @Override
    public PageInfo demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return Pages.returnPage(null);
    }
}
