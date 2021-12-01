package com.github.common.util;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public final class AsyncUti {

    public static Runnable wrapRun(Runnable runnable) {
        if (runnable == null) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // 把主线程运行时的请求和日志上下文放到异步任务的请求和日志上下文去
        return () -> {
            try {
                if (hasWeb) {
                    LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                    RequestContextHolder.setRequestAttributes(attributes);
                }
                MDC.setContextMap(A.isEmpty(contextMap) ? Collections.emptyMap() : contextMap);
                runnable.run();
            } finally {
                if (hasWeb) {
                    LocaleContextHolder.resetLocaleContext();
                    RequestContextHolder.resetRequestAttributes();
                }
                MDC.clear();
            }
        };
    }

    public static <T> Callable<T> wrapCall(Callable<T> callable) {
        if (callable == null) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // 把主线程运行时的请求和日志上下文放到异步任务的请求和日志上下文去
        return () -> {
            try {
                if (hasWeb) {
                    LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                    RequestContextHolder.setRequestAttributes(attributes);
                }
                MDC.setContextMap(A.isEmpty(contextMap) ? Collections.emptyMap() : contextMap);
                return callable.call();
            } finally {
                if (hasWeb) {
                    LocaleContextHolder.resetLocaleContext();
                    RequestContextHolder.resetRequestAttributes();
                }
                MDC.clear();
            }
        };
    }
}
