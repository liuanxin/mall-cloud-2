package com.github.product.client;

import com.github.product.constant.ProductConst;
import com.github.product.hystrix.ProductClientFallback;
import com.github.product.service.ProductService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 商品相关的调用接口
 */
@FeignClient(value = ProductConst.MODULE_NAME, fallback = ProductClientFallback.class)
public interface ProductClient extends ProductService {
}
