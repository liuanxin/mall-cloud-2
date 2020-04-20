package com.github.web;

import com.github.common.constant.CommonConst;
import com.github.common.json.JsonResult;
import com.github.common.util.U;
import com.github.global.constant.Develop;
import com.github.liuanxin.api.annotation.ApiGroup;
import com.github.liuanxin.api.annotation.ApiMethod;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiTokens;
import com.github.util.BackendDataCollectUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@ApiGroup(value = CommonConst.MODULE_INFO, index = 1)
@ApiTokens
@RestController
@RequestMapping("/common")
public class BackendCommonController {

    @ApiMethod(value = "枚举数据", develop = Develop.COMMON)
    @GetMapping("/enum")
    public JsonResult<Map<String, Map<String, Object>>> enumList(
            @ApiParam("枚举类型. 不传则返回所有列表, 多个以逗号分隔") String type) {
        return U.isBlank(type) ?
                JsonResult.success("枚举列表", BackendDataCollectUtil.ALL_ENUM_INFO) :
                JsonResult.success("枚举信息", BackendDataCollectUtil.singleEnumInfo(type));
    }
}
