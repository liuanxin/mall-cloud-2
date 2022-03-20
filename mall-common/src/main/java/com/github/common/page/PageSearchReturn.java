package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/** 使用 es 时返回的实体, 分页返回 prev 和 next, 做为深分页时上一页下一页的入参 */
@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PageSearchReturn<T> extends PageReturn<T> implements Serializable {
    private static final long serialVersionUID = 0L;

    @ApiReturn("es 返回的页面值, 请求上一页时用此值拼 page_token 的值")
    private String prevToken;

    @ApiReturn("es 返回的页面值, 请求下一页时用此值拼 page_token 的值")
    private String nextToken;
}
