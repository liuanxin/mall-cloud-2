package com.github.product.callback;

import com.github.common.page.PageReturn;
import com.github.common.util.LogUtil;
import com.github.product.client.ProductClient;
import feign.hystrix.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 商品相关的断路器
 */
@Component
public class ProductClientFallback implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable e) {
        return new ProductClient() {
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
