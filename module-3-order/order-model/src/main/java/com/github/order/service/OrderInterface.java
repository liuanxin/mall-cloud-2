package com.github.order.service;

import com.github.common.page.PageInfo;
import com.github.order.constant.OrderConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 订单相关的接口
 *
 * @author https://github.com/liuanxin
 */
public interface OrderInterface {
    
    /**
     * 示例接口
     * 
     * @param xx 参数
     * @param page 当前页
     * @param limit 每页行数
     * @return 分页信息
     */
    @GetMapping(OrderConst.ORDER_DEMO)
    PageInfo demo(@RequestParam(value = "xx", required = false) String xx,
                  @RequestParam(value = "page", required = false) Integer page,
                  @RequestParam(value = "limit", required = false) Integer limit);
}
