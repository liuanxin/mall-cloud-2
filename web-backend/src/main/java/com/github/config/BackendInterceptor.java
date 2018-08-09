package com.github.config;

import com.github.common.annotation.NeedLogin;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
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
        // 打印日志上下文中的数据
        LogUtil.RequestLogContext logContextInfo = RequestUtils.logContextInfo()
                .setId(String.valueOf(BackendSessionUtil.getUserId()))
                .setName(BackendSessionUtil.getUserName());
        LogUtil.bind(logContextInfo);
    }

    private void unbindParam() {
        // 删除打印日志上下文中的数据
        LogUtil.unbind();
    }

    /** 检查登录 */
    private void checkLoginAndPermission(Object handler) {
        NeedLogin needLogin = getAnnotation((HandlerMethod) handler, NeedLogin.class);
        // 标注了 @NeedLogin 且 flag 为 true(默认就是 true)则表示当前请求需要登录
        if (needLogin != null && needLogin.flag()) {
            BackendSessionUtil.checkLogin();
        }
    }
    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> clazz) {
        // 先找方法上的注解, 没有再找类上的注解
        T annotation = handlerMethod.getMethodAnnotation(clazz);
        return annotation == null ? AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), clazz) : annotation;
    }
}
