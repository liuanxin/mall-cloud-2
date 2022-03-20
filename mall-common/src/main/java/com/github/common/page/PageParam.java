package com.github.common.page;

import com.github.common.util.U;
import com.github.liuanxin.api.annotation.ApiParam;
import com.github.liuanxin.api.annotation.ApiParamIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

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
