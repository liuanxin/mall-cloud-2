package com.github.config;

import com.github.common.annotation.NotNeedLogin;
import com.github.common.annotation.NotNeedPermission;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.util.ManagerSessionUtil;
import com.google.common.collect.Lists;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.List;

public class ManagerInterceptor implements HandlerInterceptor {

    private static final List<String> LET_IT_GO = Lists.newArrayList("/error");

    private boolean online;
    ManagerInterceptor(boolean online) {
        this.online = online;
    }

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
                .setId(String.valueOf(ManagerSessionUtil.getUserId()))
                .setName(ManagerSessionUtil.getUserName());
        LogUtil.bind(logContextInfo);
    }

    private void unbindParam() {
        // 删除打印日志上下文中的数据
        LogUtil.unbind();
    }

    /** 检查登录及权限 */
    private void checkLoginAndPermission(Object handler) {
        if (!online) {
            return;
        }
        if (LET_IT_GO.contains(RequestUtils.getRequest().getRequestURI())) {
            return;
        }
        if (!handler.getClass().isAssignableFrom(HandlerMethod.class)) {
            return;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 在不需要登录的 url 上标注 @NotNeedLogin
        NotNeedLogin notNeedLogin = getAnnotation(handlerMethod, NotNeedLogin.class);
        // 标注了 NotNeedLogin 且 flag 为 true(默认就是 true)则表示当前的请求不需要验证登录
        if (notNeedLogin != null && notNeedLogin.flag()) {
            return;
        }
        // 检查登录
        ManagerSessionUtil.checkLogin();

        // 在不需要验证权限的 url 上标注 @NotNeedPermission
        NotNeedPermission notNeedPermission = getAnnotation(handlerMethod, NotNeedPermission.class);
        // 标注了 NotNeedPermission 且 flag 为 true(默认就是 true)则表示当前的请求不需要验证权限
        if (notNeedPermission != null && notNeedPermission.flag()) {
            return;
        }
        // 检查权限
        ManagerSessionUtil.checkPermission();
    }
    private <T extends Annotation> T getAnnotation(HandlerMethod handlerMethod, Class<T> clazz) {
        // 先找方法上的注解, 没有再找类上的注解
        T annotation = handlerMethod.getMethodAnnotation(clazz);
        return annotation == null ? AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), clazz) : annotation;
    }
}
