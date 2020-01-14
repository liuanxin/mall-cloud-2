package com.github.common.page;

import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiParamIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

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
public class Page implements Serializable {
    private static final long serialVersionUID = 0L;

    /** 前台传递过来的分页参数名 */
    public static final String GLOBAL_PAGE = "page";
    /** 前台传递过来的每页条数名 */
    public static final String GLOBAL_LIMIT = "limit";

    /** 分页默认页 */
    private static final int DEFAULT_PAGE_NO = 1;
    /** 分页默认的每页条数 */
    private static final int DEFAULT_LIMIT = 20;
    /** 最大分页条数 */
    private static final int MAX_LIMIT = 1000;

    @ApiParam("当前页数. 不传或传入 0, 或负数, 或非数字则默认是 1")
    private int page;

    @ApiParam("每页条数. 不传或传入 0, 或负数, 或非数字, 或大于 " + MAX_LIMIT + " 则默认是 " + DEFAULT_LIMIT)
    private int limit;

    /** 是否是移动端 */
    @ApiParamIgnore
    private boolean wasMobile = false;

    public Page() {
        this.page = DEFAULT_PAGE_NO;
        this.limit = DEFAULT_LIMIT;
    }

    public Page(String page, String limit) {
        this.page = handlerPage(page);
        this.limit = handlerLimit(limit);
    }

    public Page(int page, int limit) {
        this.page = handlerPage(page);
        this.limit = handlerLimit(limit);
    }

    public int start() {
        return (page - 1) * limit;
    }

    public static int handlerPage(String page) {
        return handlerPage(U.toInt(page));
    }
    public static int handlerPage(int page) {
        return page <= 0 ? DEFAULT_PAGE_NO : page;
    }

    public static int handlerLimit(String limit) {
        return handlerLimit(U.toInt(limit));
    }
    public static int handlerLimit(int limit) {
        return (limit <= 0 || limit > MAX_LIMIT) ? DEFAULT_LIMIT : limit;
    }
}
