package com.github.common.client;

import com.github.common.callback.CommonClientFallback;
import com.github.common.constant.CommonConst;
import com.github.common.service.CommonService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 公共相关的调用接口
 */
@FeignClient(value = CommonConst.MODULE_NAME, fallbackFactory = CommonClientFallback.class)
public interface CommonClient extends CommonService {
}
