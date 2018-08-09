package com.github.search.client;

import com.github.search.constant.SearchConst;
import com.github.search.hystrix.SearchFallback;
import com.github.search.service.SearchInterface;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 搜索相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = SearchConst.MODULE_NAME, fallback = SearchFallback.class)
public interface SearchService extends SearchInterface {
}
