package com.github.web;

import com.github.common.annotation.NeedLogin;
import com.github.common.json.JsonResult;
import com.github.common.mvc.AppTokenHandler;
import com.github.common.util.U;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.user.constant.UserConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiGroup(UserConst.MODULE_INFO)
@RestController
public class BackendUserController {

    @NeedLogin
    @ApiMethod(title = "刷新 token", develop = Develop.USER, desc = "每次打开 app 且本地有 token 值就请求此接口(pc 无视)")
    @GetMapping("/refresh-token")
    public JsonResult<String> index() {
        String refreshToken = AppTokenHandler.resetTokenExpireTime();
        if (U.isBlank(refreshToken)) {
            return JsonResult.fail("token 刷新失败, 请重新登录!");
        } else {
            return JsonResult.success("token 刷新成功!", refreshToken);
        }
    }
}
