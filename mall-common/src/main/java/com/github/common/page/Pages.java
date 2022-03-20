package com.github.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.common.util.A;
import com.github.common.util.U;

import java.util.Collections;
import java.util.List;

/** 当 controller 层无需引用 mbp 包时只需要在 service 层调用当前工具类 */
public final class Pages {

    /** 在 service 的实现类中调用 --> 当不想查 select count(*) 时用这个 */
    public static <T> Page<T> paramOnlyLimit(long limit) {
        return new Page<>(1, limit, false);
    }

    public static <T> List<T> returnList(Page<T> pageInfo) {
        return U.isNull(pageInfo) ? Collections.emptyList() : pageInfo.getRecords();
    }

    public static <T> T returnOne(Page<T> pageInfo) {
        return U.isNull(pageInfo) ? null : A.first(pageInfo.getRecords());
    }

    public static <T> boolean hasExists(Page<T> pageInfo) {
        return U.isNotNull(pageInfo) && U.isNotNull(A.first(pageInfo.getRecords()));
    }

    public static <T> boolean notExists(Page<T> pageInfo) {
        return U.isNull(pageInfo) || U.isNull(A.first(pageInfo.getRecords()));
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
    public static <T> PageReturn<T> returnPage(Page<T> pageInfo) {
        if (U.isNull(pageInfo)) {
            return PageReturn.emptyReturn();
        } else {
            return PageReturn.returnPage(pageInfo.getTotal(), pageInfo.getRecords());
        }
    }
}
