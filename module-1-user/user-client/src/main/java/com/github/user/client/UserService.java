package com.github.user.client;

import com.github.user.service.UserInterface;
import com.github.user.constant.UserConst;
import com.github.user.hystrix.UserFallback;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 用户相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = UserConst.MODULE_NAME, fallback = UserFallback.class)
public interface UserService extends UserInterface {
}
