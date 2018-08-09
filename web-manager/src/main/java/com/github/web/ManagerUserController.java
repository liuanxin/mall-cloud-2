package com.github.web;

import com.github.common.json.JsonResult;
import com.github.common.page.Page;
import com.github.common.page.PageInfo;
import com.github.dto.DemoDto;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.user.client.UserService;
import com.github.vo.DemoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@ApiGroup({"user-用户"})
public class ManagerUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/demo")
    @ApiMethod(title = "示例", develop = "liuanxin")
    public JsonResult<PageInfo<DemoVo>> demo(@ApiParam("用户名") String name, DemoDto dto, Page page) {
        userService.demo(name, page.getPage(), page.getLimit());
        return JsonResult.success("xx", null);
    }
}
