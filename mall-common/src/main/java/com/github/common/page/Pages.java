package com.github.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.common.util.A;
import com.github.common.util.U;

import java.util.List;

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
public final class Pages {

    /** 在 service 的实现类中调用 --> 当不想查 select count(*) 时用这个 */
    public static <T> Page<T> paramOnlyLimit(long limit) {
        return new Page<>(1, limit, false);
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的参数是 mbp 的 Page 对象, service 上的参数是 PageParam, 使用此方法进行转换 */
    public static <T> Page<T> param(PageParam page) {
        // 移动端与 pc 端的分页不同, 前者的用户习惯是一直刷, 一边刷一边加载, 它是不需要查询 select count(*) 的
        // 移动端也不需要有当前页的概念, 如果数据是按时间倒序(时间越后越排在前), 从下往上刷时, 它只需要加载比最下面的时间小的数据即可
        return page.isWasMobile()
                ? paramOnlyLimit(page.getLimit())
                : new Page<>(page.getPage(), page.getLimit());
    }

    /** 在 service 的实现类中调用 --> 在 repository 方法上的返回类型是 mbp 的 Page 对象, service 上的返回类型是 PageReturn, 使用此方法进行转换 */
    public static <T> PageReturn<T> returnPage(Page<T> pageObj) {
        if (U.isBlank(pageObj)) {
            return PageReturn.emptyReturn();
        } else {
            List<T> objList = pageObj.getRecords();
            return A.isEmpty(objList) ? PageReturn.emptyReturn() : PageReturn.returnPage(pageObj.getTotal(), objList);
        }
    }
}
