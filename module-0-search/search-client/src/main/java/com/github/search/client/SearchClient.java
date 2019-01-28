package com.github.search.client;

import com.github.search.constant.SearchConst;
import com.github.search.hystrix.SearchClientFallback;
import com.github.search.service.SearchService;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * 搜索相关的调用接口
 *
 * @author https://github.com/liuanxin
 */
@FeignClient(value = SearchConst.MODULE_NAME, fallback = SearchClientFallback.class)
public interface SearchClient extends SearchService {
}
