package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiParam;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/** 使用 es 时返回的实体, 入参添加 pageToken 用来处理深分页 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageSearchParam extends PageParam implements Serializable {
    private static final long serialVersionUID = 0L;

    @ApiParam("使用 es 查询时返回的页面值, 接口有返回就拼上此值(上页使用 prev_token, 下页使用 next_token)")
    private String pageToken;
}
