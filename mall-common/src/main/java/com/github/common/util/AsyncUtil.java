package com.github.common.util;

import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

@SuppressWarnings("DuplicatedCode")
public final class AsyncUtil {

    /**
     * 直接用 Executors 的静态方法生成的线程池, 想要线程池里的线程共享主线程的上下文, 构造时使用此方法生成的 ThreadFactory<p/>
     *
     * 如 <code>Executors.newSingleThreadExecutor(AsyncUtil.wrapThreadFactory())</code>
     */
    public static ThreadFactory wrapThreadFactory() {
        return runnable -> new Thread(wrapRunContext(runnable));
    }

    /** 线程想要共享主线程的上下文, 使用此方法 */
    public static Runnable wrapRunContext(Runnable runnable) {
        if (U.isNull(runnable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> logContextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(logContextMap);

        if (hasWeb || hasLogContext) {
            // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
            return () -> {
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(logContextMap);
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

    /** 回调线程想要共享主线程的上下文, 使用此方法 */
    public static <T> Callable<T> wrapCallContext(Callable<T> callable) {
        if (U.isNull(callable)) {
            return null;
        }

        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        boolean hasWeb = (attributes instanceof ServletRequestAttributes);

        Map<String, String> logContextMap = MDC.getCopyOfContextMap();
        boolean hasLogContext = A.isNotEmpty(logContextMap);

        if (hasWeb || hasLogContext) {
            return () -> {
                // 把主线程运行时的请求和日志上下文放到子线程的请求和日志上下文去
                try {
                    if (hasLogContext) {
                        MDC.setContextMap(logContextMap);
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
