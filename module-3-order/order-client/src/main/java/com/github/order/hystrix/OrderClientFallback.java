package com.github.order.hystrix;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.order.client.OrderClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 订单相关的断路器
 */
@Component
public class OrderClientFallback implements FallbackFactory<OrderClient> {

    @Override
    public OrderClient create(Throwable e) {
        return new OrderClient() {
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
