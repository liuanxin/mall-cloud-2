package com.github.web;

import com.github.common.json.JsonResult;
import com.github.common.util.SecurityCodeUtil;
import com.github.common.util.U;
import com.github.util.BackendDataCollectUtil;
import com.github.util.BackendSessionUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class BackendIndexController {

    @ResponseBody
    @GetMapping("/")
    public String index() {
        return "api-gateway";
    }

    @GetMapping("/enum")
    @ResponseBody
    public JsonResult enumList(String type) {
        return U.isBlank(type) ?
                JsonResult.success("枚举列表", BackendDataCollectUtil.ALL_ENUM_INFO) :
                JsonResult.success("枚举信息", BackendDataCollectUtil.singleEnumInfo(type));
    }

    @GetMapping("/code")
    public void code(HttpServletResponse response, String width, String height,
                     String count, String style) throws IOException {
        SecurityCodeUtil.Code code = SecurityCodeUtil.generateCode(count, style, width, height);

        // 往 session 里面丢值
        BackendSessionUtil.putImageCode(code.getContent());

        // 向页面渲染图像
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/jpeg");
        javax.imageio.ImageIO.write(code.getImage(), "jpeg", response.getOutputStream());
    }
}