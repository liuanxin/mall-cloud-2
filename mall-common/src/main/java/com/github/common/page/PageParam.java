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
 * public JsonResult xx(xxx, PageParam page) {
 *     PageReturn PageReturn = xxxService.page(xxx, page);
 *     return success("xxx", (page.isWasMobile() ? PageReturn.getList() : PageReturn));
 * }
 *
 * &#064;Service --> 调用方法使用 Page 进行传递, 返回 PageReturn
 * public PageReturn page(xxx, PageParam page) {
 *     List&lt;XXX> xxxList = xxxMapper.selectPage(Pages.param(page), xxxxx);
 *     return Pages.returnPage(xxxList);
 * }
 *
 * 这么做的目的是分页包只需要在服务端引入即可
 * </pre>
 */
@Setter
@Getter
public class PageParam implements Serializable {
    private static final long serialVersionUID = 0L;

    /** 前台传递过来的分页参数名 */
    public static final String GLOBAL_PAGE = "page";
    /** 前台传递过来的每页条数名 */
    public static final String GLOBAL_LIMIT = "limit";

    /** 分页默认页 */
    private static final int DEFAULT_PAGE_NO = 1;
    /** 分页默认的每页条数 */
    private static final int DEFAULT_LIMIT = 10;
    /** 最大分页条数 */
    private static final int MAX_LIMIT = 1000;

    @ApiParam("当前页数. 不传 或 传入负数 或 传入非数字 则默认是 " + DEFAULT_PAGE_NO)
    private int page;

    @ApiParam("每页条数. 不传 或 传入负数 或 传入非数字 或 传入大于 " + MAX_LIMIT + " 的数则默认是 " + DEFAULT_LIMIT)
    private int limit;

    @ApiParam("使用 es 查询时会返回此值, 请求下一页(仅下一页, 不能跳页, 也不能上一页)时这个如果有值, 带上即可")
    private String searchAfter;

    /** 是否是移动端 */
    @ApiParamIgnore
    private boolean wasMobile = false;

    public PageParam() {
        this.page = DEFAULT_PAGE_NO;
        this.limit = DEFAULT_LIMIT;
    }

    public PageParam(String page, String limit) {
        this.page = handlerPage(page);
        this.limit = handlerLimit(limit);
    }

    public PageParam(int page, int limit) {
        this.page = handlerPage(page);
        this.limit = handlerLimit(limit);
    }

    /** 分页语句  LIMIT x, xx  中  x  的值 */
    public int pageStart() {
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
