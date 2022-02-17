package com.github.common.util;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public final class AsyncUti {

    public static Runnable wrapRun(Runnable runnable) {
        if (U.isNull(runnable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(contextMap);

        if (hasWeb || hasLogContext) {
            // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
            return () -> {
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(contextMap);
                    }
                    if (hasWeb) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    runnable.run();
                } finally {
                    if (hasWeb) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (hasLogContext) {
                        MDC.clear();
                    }
                }
            };
        } else {
            return runnable;
        }
    }

    public static <T> Callable<T> wrapCall(Callable<T> callable) {
        if (U.isNull(callable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(contextMap);

        if (hasWeb || hasLogContext) {
            return () -> {
                // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(contextMap);
                    }
                    if (hasWeb) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    return callable.call();
                } finally {
                    if (hasWeb) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    if (hasLogContext) {
                        MDC.clear();
                    }
                }
            };
        } else {
            return callable;
        }
    }
}
