package com.github.order.client;

import com.github.order.constant.OrderConst;
import com.github.order.hystrix.OrderClientFallback;
import com.github.order.service.OrderService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 订单相关的调用接口
 */
@FeignClient(value = OrderConst.MODULE_NAME, fallback = OrderClientFallback.class)
public interface OrderClient extends OrderService {
}
