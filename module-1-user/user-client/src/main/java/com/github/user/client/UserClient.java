package com.github.user.client;

import com.github.user.constant.UserConst;
import com.github.user.hystrix.UserClientFallback;
import com.github.user.service.UserService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 用户相关的调用接口
 */
@FeignClient(value = UserConst.MODULE_NAME, fallback = UserClientFallback.class)
public interface UserClient extends UserService {
}
