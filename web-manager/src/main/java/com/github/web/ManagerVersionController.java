package com.github.web;

import com.github.common.mvc.ApiVersion;
import com.github.common.mvc.AppVersion;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RequestMapping("/version")
@Controller
public class ManagerVersionController {

    private static final String URL = "/tmp";

    /** 「错误的版本号」或「不带版本号」的请求路由至此 */
    @ResponseBody
    @GetMapping(URL)
    public String v() {
        return "example";
    }

    @ResponseBody
    @GetMapping(URL)
    @ApiVersion(AppVersion.V101)
    public String v2() {
        return "example: " + AppVersion.V101;
    }

    @ResponseBody
    @GetMapping(URL)
    @ApiVersion(AppVersion.V104)
    public String v5() {
        return "example: " + AppVersion.V104;
    }
}
