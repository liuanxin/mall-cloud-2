package com.github.common.page;

import com.github.common.util.A;
import com.github.liuanxin.page.model.PageBounds;
import com.github.liuanxin.page.model.PageList;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * 此实体类只在 <span style="color:red">Service</span> 中用到分页时使用.
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
 * <span style="color:red">Controller 中不要使用此类</span>, mybatis 的分页插件只需要在服务端引入.
 * </pre>
 */
public final class Pages {

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换 */
    public static PageBounds param(Page page) {
        return new PageBounds(page.getPage(), page.getLimit());
    }

    /**
     * 在 service 的实现类中调用 --> 在 repository 方法上的参数是 PageBounds, service 上的参数是 Page, 使用此方法进行转换.
     * 此方法会自动基于当前请求是 pc 还是手机来忽略请求总条数(select count(*) ...), 手机上不需要构建页码只需要上下刷, 因此不需要.
     * 手机上也是不需要页面的, 只需要头尾两条数据的信息(比如 > 最下面记录的 id 或者 < 最上面记录的 id)
     */
    public static PageBounds autoParam(Page page) {
        if (page.isWasMobile()) {
            PageBounds bounds = new PageBounds(page.getLimit());
            bounds.setQueryTotal(false);
            return bounds;
        } else {
            return param(page);
        }
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的返回类型是 List, service 上的返回类型是 PageInfo, 使用此方法进行转换 */
    public static <T> PageInfo<T> returnPage(List<T> list) {
        if (A.isEmpty(list)) {
            return PageInfo.emptyReturn();
        } else if (list instanceof PageList) {
            return PageInfo.returnPage(((PageList) list).getTotal(), new ArrayList<T>(list));
        } else {
            return PageInfo.returnPage(list.size(), list);
        }
    }
}
