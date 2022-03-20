package com.github.search.config;

import com.github.common.Const;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 搜索模块的 web 拦截器
 */
public class SearchInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        
        String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
        LogUtil.putContext(traceId, RequestUtil.logContextInfo());
        LogUtil.putIp(RequestUtil.getRealIp());
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
            if (LogUtil.ROOT_LOG.isDebugEnabled())
                LogUtil.ROOT_LOG.debug("request was over, but have exception: " + ex.getMessage());
        }
        LogUtil.unbind();
    }
}
