package com.github.common.client;

import com.github.common.service.CommonInterface;
import com.github.common.constant.CommonConst;
import com.github.common.hystrix.CommonFallback;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 公共相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = CommonConst.MODULE_NAME, fallback = CommonFallback.class)
public interface CommonService extends CommonInterface {
}
