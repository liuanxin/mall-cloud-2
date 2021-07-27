package com.github.product.hystrix;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.product.client.ProductClient;
import org.springframework.stereotype.Component;

/**
 * 商品相关的断路器
 */
@Component
public class ProductClientFallback implements ProductClient {

    @Override
    public PageReturn demo(String xx, Integer page, Integer limit) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("调用断路器");
        }
        return null;
    }
}
