package com.github.config;

import com.github.common.Const;
import com.github.common.annotation.NeedLogin;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.util.BackendSessionUtil;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;

public class BackendInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        // 跟子线程共享请求上下文, 这样之后 FeignInterceptor 中调用 RequestContextHolder.getRequestAttributes() 才不是 null
        // RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
        bindParam();

        checkLoginAndPermission(handler);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        if (ex != null) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("request was over, but have exception: " + ex.getMessage());
            }
        }
        unbindParam();
    }

    private void bindParam() {
        String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
        LogUtil.putContext(traceId, RequestUtil.logContextInfo());
        LogUtil.putIp(RequestUtil.getRealIp());
        LogUtil.putUser(BackendSessionUtil.getUserInfo());
    }

    private void unbindParam() {
        // 删除打印日志上下文中的数据
        LogUtil.unbind();
    }

    /** 检查登录 */
    private void checkLoginAndPermission(Object handler) {
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            return;
        }

        NeedLogin needLogin = getAnnotation((HandlerMethod) handler, NeedLogin.class);
        // 标注了 @NeedLogin 且 flag 为 true(默认就是 true)则表示当前请求需要登录
        if (needLogin != null && needLogin.value()) {
            BackendSessionUtil.checkLogin();
        }
    }
    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> clazz) {
        // 先找方法上的注解, 没有再找类上的注解
        T annotation = handlerMethod.getMethodAnnotation(clazz);
        return annotation == null ? AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), clazz) : annotation;
    }
}
