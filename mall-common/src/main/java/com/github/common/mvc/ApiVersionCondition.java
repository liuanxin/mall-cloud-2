package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.util.U;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import javax.servlet.http.HttpServletRequest;

public class ApiVersionCondition implements RequestCondition<ApiVersionCondition> {

    private AppVersion version;

    /** 参数 version 表示: 标注在 controller 方法上的注解 ApiVersion 里面的值 */
    public ApiVersionCondition(AppVersion version) {
        this.version = version;
    }

    @Override
    public ApiVersionCondition combine(ApiVersionCondition other) {
        return new ApiVersionCondition(other.version);
    }

    @Override
    public ApiVersionCondition getMatchingCondition(HttpServletRequest request) {
        // 从请求中获取版本信息
        String version = request.getHeader(Const.VERSION);
        if (U.isBlank(version)) {
            version = request.getParameter(Const.VERSION);
        }
        AppVersion appVersion = U.toEnum(AppVersion.class, version);
        // 如果前台过来的参数是 v3, 版本里面有 v1 v2 v4 v5, 最后 v1 v2 会被匹配上
        return (appVersion != null && appVersion.greaterOrEqual(this.version)) ? this : null;
    }

    /**
     * 从上面的匹配中将会导致匹配到多个, 如上面的 v3, 将会返回三个: v v1 v2.<br>
     * spring 会基于下面这个方法返回的值做排序, 然后将排序后的第一个方法做为最佳匹配, 如果多于一个则将第二个做为第二匹配.<br>
     * 而后将第一匹配和第二匹配再按照这个方法进行比较. 如果两个匹配的比较结果一致, 将会抛出两个方法对于这个请求太过暧昧的异常.<br>
     * 将最佳匹配做为请求的处理方法去执行!
     *
     * @see org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#lookupHandlerMethod
     */
    @Override
    public int compareTo(ApiVersionCondition other, HttpServletRequest request) {
        return (other != null && version != null) ? (other.version.getCode() - version.getCode()) : 0;
    }
}
