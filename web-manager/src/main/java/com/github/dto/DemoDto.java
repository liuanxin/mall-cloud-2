package com.github.dto;

import com.github.common.enums.Gender;
import com.github.liuanxin.api.annotation.ApiParam;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DemoDto {

    @ApiParam(value = "用户 id", required = true)
    private Long userId;

    @ApiParam(value = "性别", dataType = "int")
    private Gender gender;
}
