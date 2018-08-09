package com.github.order.client;

import com.github.order.service.OrderInterface;
import com.github.order.constant.OrderConst;
import com.github.order.hystrix.OrderFallback;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 订单相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = OrderConst.MODULE_NAME, fallback = OrderFallback.class)
public interface OrderService extends OrderInterface {
}
