package com.github.search.client;

import com.github.search.callback.SearchClientFallback;
import com.github.search.constant.SearchConst;
import com.github.search.service.SearchService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 搜索相关的调用接口
 */
@FeignClient(value = SearchConst.MODULE_NAME, fallbackFactory = SearchClientFallback.class)
public interface SearchClient extends SearchService {
}
