package com.github.common.page;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.common.json.JsonUtil;
import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@SuppressWarnings({"unchecked", "rawtypes"})
public class PageReturn<T> implements Serializable {
    private static final long serialVersionUID = 0L;

    private static final PageReturn EMPTY = new PageReturn<>(0, Collections.emptyList());

    @ApiReturn("SELECT COUNT(*) FROM ... 的结果")
    private long total;

    @ApiReturn("SELECT ... FROM ... LIMIT 0, 10 的结果")
    private List<T> list;

    public PageReturn(long total) {
        this.total = total;
    }

    public static <T> PageReturn<T> emptyReturn() {
        return EMPTY;
    }
    public static <T> PageReturn<T> returnTotal(long total) {
        return new PageReturn<>(total, Collections.emptyList());
    }
    public static <T> PageReturn<T> returnPage(long total, List<T> list) {
        return new PageReturn<>(total, list);
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convertJustTotal(PageReturn<S> pageInfo) {
        if (U.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal());
        }
    }

    /** 在 Controller 中调用 --> 组装不同的 res 时使用此方法 */
    public static <S,T> PageReturn<T> convert(PageReturn<S> pageInfo, Class<T> clazz) {
        if (U.isNull(pageInfo)) {
            return EMPTY;
        } else {
            return new PageReturn<>(pageInfo.getTotal(), JsonUtil.convertList(pageInfo.getList(), clazz));
        }
    }
}
