package com.github.common.page;

import com.github.liuanxin.api.annotation.ApiReturn;
import com.github.common.util.U;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 * 此实体类在 Controller 和 Service 中用到分页时使用.
 *
 * &#064;Controller --> request 请求中带过来的参数使用 Page 进行接收(如果前端不传, 此处接收则程序会使用默认值)
 * public JsonResult xx(xxx, Page page) {
 *     PageInfo pageInfo = xxxService.page(xxx, page);
 *     return success("xxx", (page.isWasMobile() ? pageInfo.getList() : pageInfo));
 * }
 *
 * &#064;Service --> 调用方法使用 Page 进行传递, 返回 PageInfo
 * public PageInfo page(xxx, Page page) {
 *     PageBounds pageBounds = Pages.param(page);
 *     List&lt;XXX> xxxList = xxxMapper.selectByExample(xxxxx, pageBounds);
 *     return Pages.returnPage(xxxList);
 * }
 *
 * 这么做的目的是分页包只需要在服务端引入即可
 * </pre>
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo<T> implements Serializable {
    private static final long serialVersionUID = 0L;

    @ApiReturn("总条数. 根据此值和 page limit 构建分页按钮")
    private int total;

    @ApiReturn("当前页的数据")
    private List<T> list;

    public static <T> PageInfo<T> emptyReturn() {
        return new PageInfo<T>(0, Collections.emptyList());
    }
    public static <T> PageInfo<T> returnPage(int total, List<T> list) {
        return new PageInfo<T>(total, list);
    }

    /** 在 Controller 中调用 --> 组装不同的 vo 时使用此方法 */
    public static <S,T> PageInfo<T> convert(PageInfo<S> pageInfo) {
        if (U.isBlank(pageInfo)) {
            return emptyReturn();
        } else {
            // 只要总条数
            PageInfo<T> info = new PageInfo<T>();
            info.setTotal(pageInfo.getTotal());
            return info;
        }
    }
}
