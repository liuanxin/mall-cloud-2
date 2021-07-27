package com.github.order.hystrix;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.order.client.OrderClient;
import org.springframework.stereotype.Component;

/**
 * 订单相关的断路器
 */
@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public PageReturn demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return null;
    }
}
