package com.github.web;

import com.github.common.json.JsonResult;
import com.github.common.page.PageParam;
import com.github.common.page.PageReturn;
import com.github.dto.DemoDto;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.user.constant.UserConst;
import com.github.user.service.UserService;
import com.github.vo.DemoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@ApiGroup(UserConst.MODULE_INFO)
public class ManagerUserController {

    private final UserService userService;

    @ApiMethod("示例")
    @GetMapping("/demo")
    public JsonResult<PageReturn<DemoVo>> demo(@ApiParam("用户名") String name, DemoDto dto, PageParam page) {
        userService.demo(name, page.getPage(), page.getLimit());
        return JsonResult.success("xx", null);
    }
}
