package com.github.user.service;

import com.github.common.page.PageReturn;
import com.github.user.constant.UserConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户相关的接口
 */
public interface UserService {

    /**
     * 示例接口
     *
     * @param xx 参数
     * @param page 当前页
     * @param limit 每页行数
     * @return 分页信息
     */
    @GetMapping(UserConst.USER_DEMO)
    PageReturn demo(@RequestParam(value = "xx", required = false) String xx,
                    @RequestParam(value = "page", required = false) Integer page,
                    @RequestParam(value = "limit", required = false) Integer limit);
}
