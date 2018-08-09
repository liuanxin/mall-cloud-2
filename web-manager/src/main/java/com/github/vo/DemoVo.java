package com.github.vo;

import com.github.common.enums.Gender;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
public class DemoVo {

    @ApiReturn("用户 id")
    private String userId;

    @ApiParam(value = "性别", dataType = "int")
    private Gender gender;
}
