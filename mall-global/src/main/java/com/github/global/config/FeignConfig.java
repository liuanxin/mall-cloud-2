package com.github.global.config;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import feign.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 处理 feign 的请求头、日志打印、MDC 上下文 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ FeignClient.class, Feign.class })
public class FeignConfig {

    private static final Set<String> IGNORE_HEADER_SET = Sets.newHashSet("content-type", "content-length");

    @Value("${json.sufferErrorRequest:true}")
    private boolean sufferErrorRequest;

    @Value("${json.logPrintHeader:false}")
    private boolean printHeader;

    private final JsonDesensitization jsonDesensitization;

    /** 处理请求头: 把 trace_id 放到 Feign 的请求上下文中去(feign 默认会将当前上下文中的头放到自身的头里) */
    @Bean
    @ConditionalOnClass(HttpServletRequest.class)
    public RequestInterceptor handleHeader() {
        return template -> {
            // 将当前请求上下文的 header 信息放到请求 feign 的 header 中去
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

                Enumeration<String> headers = request.getHeaderNames();
                while (headers.hasMoreElements()) {
                    String headName = headers.nextElement();
                    if (!IGNORE_HEADER_SET.contains(headName.toLowerCase())) {
                        String headerValue = request.getHeader(headName);
                        if (U.isNotEmpty(headerValue)) {
                            template.header(headName, Collections.emptyList()).header(headName, headerValue);
                        }
                    }
                }
                // 将跟踪号放到请求上下文(上面的请求头中如果没有就从 request 的属性中获取)
                if (U.isEmpty(request.getHeader(Const.TRACE))) {
                    String traceId = LogUtil.getTraceId();
                    if (U.isNotEmpty(traceId)) {
                        // 先清空再设置
                        template.header(Const.TRACE, Collections.emptyList()).header(Const.TRACE, traceId);
                    }
                }
            }
        };
    }

    /**
     * 处理日志打印: 需要配置 feign.client.config.default.loggerLevel 的值才能进入下面的日志打印,
     * 这个值默认是 NONE, 只要不是 NONE 就行(可以设置为 BASIC、HEADERS、FULL),
     * 默认的日志会输出很多条, 见 {@link Logger}, 当前处理是只在请求前打印一条, 有响应时打印一条, io 异常时打印一条
     */
    @Bean
    @SuppressWarnings("DuplicatedCode")
    public Logger handleLog() {
        return new Logger() {
            @Override
            protected void log(String configKey, String format, Object... args) {}

            @Override
            protected void logRequest(String configKey, Level logLevel, Request request) {
                StringBuilder sbd = new StringBuilder("request:[");
                sbd.append("url(").append(request.url()).append(")");
                if (printHeader) {
                    sbd.append("header(");
                    for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet()) {
                        sbd.append("<").append(entry.getKey()).append(": ").append(entry.getValue()).append(">");
                    }
                    sbd.append(")");
                }

                byte[] body = request.body();
                if (body != null && body.length > 0) {
                    String data = request.isBinary() ? "Binary data" : new String(body, request.charset());
                    sbd.append(" body(").append(jsonDesensitization.toJson(data)).append(")");
                }
                sbd.append("]");
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("feignClient --> {} -> {}", methodTag(configKey), sbd);
                }
            }

            @Override
            protected void logRetry(String configKey, Level logLevel) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("feignClient --> {} -> retrying...", methodTag(configKey));
                }
            }

            @Override
            protected Response logAndRebufferResponse(String configKey, Level level, Response response, long useTime) {
                StringBuilder sbd = new StringBuilder("response:[");
                sbd.append("time(").append(useTime).append(" ms) ").append(response.status());
                if (U.isNotBlank(response.reason())) {
                    sbd.append(' ').append(response.reason());
                }
                if (printHeader) {
                    sbd.append("header(");
                    for (Map.Entry<String, Collection<String>> entry : response.headers().entrySet()) {
                        sbd.append("<").append(entry.getKey()).append(": ").append(entry.getValue()).append(">");
                    }
                    sbd.append(")");
                }
                if (response.body() != null && response.body().isRepeatable()) {
                    sbd.append(" return(");
                    try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
                        sbd.append(jsonDesensitization.toJson(CharStreams.toString(reader)));
                    } catch (Exception ignore) {
                    }
                    sbd.append(")");
                }

                if (response.body() != null) {
                    if (response.body().isRepeatable()) {
                        sbd.append(" return(");
                        try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
                            sbd.append(jsonDesensitization.toJson(CharStreams.toString(reader)));
                        } catch (Exception ignore) {
                        }
                        sbd.append(")");
                    } else if (sufferErrorRequest) {
                        try (InputStream inputStream = response.body().asInputStream()) {
                            byte[] bytes = ByteStreams.toByteArray(inputStream);

                            sbd.append(" return(");
                            sbd.append(jsonDesensitization.toJson(new String(bytes)));
                            sbd.append(")");

                            response = Response.builder().status(response.status()).reason(response.reason())
                                    .headers(response.headers()).request(response.request()).body(bytes).build();
                        } catch (Exception e) {
                            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                LogUtil.ROOT_LOG.error("feignClient 数据转换失败", e);
                            }
                        }
                    }
                }

                sbd.append("]");
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("feignClient <-- {} <- {}", methodTag(configKey), sbd);
                }
                return response;
            }

            @Override
            protected IOException logIOException(String configKey, Level level, IOException e, long useTime) {
                StringBuilder sbd = new StringBuilder("exception:[");
                sbd.append("time(").append(useTime).append(" ms) ");
                sbd.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
                sbd.append("]");
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info(String.format("feignClient <--> %s <-> %s", methodTag(configKey), sbd), e);
                }
                return e;
            }
        };
    }

    /** 处理 mdc: 把主线程的日志上下文放到 feign 的日志上下文中去 */
    @Bean
    public HystrixConcurrencyStrategy handleMdc() {
        return new AfterSaleFeignConcurrencyStrategy();
    }

    public static class AfterSaleFeignConcurrencyStrategy extends HystrixConcurrencyStrategy {
        private final HystrixConcurrencyStrategy originalStrategy;

        public AfterSaleFeignConcurrencyStrategy() {
            // 用原来的策略返回
            this.originalStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();

            // 保留现有 Hystrix 插件的引用
            HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();
            HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
            HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
            HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();

            HystrixPlugins.reset();

            // 注册现有 Hystrix 插件, 并发策略插件除外
            HystrixPlugins.getInstance().registerConcurrencyStrategy(this);
            HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);
            HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
            HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
            HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        }

        @Override
        public <T> Callable<T> wrapCallable(Callable<T> callable) {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            boolean hasRequest = (attributes instanceof ServletRequestAttributes);

            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            // 把主线程运行时的请求和日志上下文放到 feign 的请求和日志上下文去
            return () -> {
                try {
                    if (hasRequest) {
                        LocaleContextHolder.setLocale(((ServletRequestAttributes) attributes).getRequest().getLocale());
                        RequestContextHolder.setRequestAttributes(attributes);
                    }
                    MDC.setContextMap(A.isEmpty(contextMap) ? Collections.emptyMap() : contextMap);
                    return callable.call();
                } finally {
                    if (hasRequest) {
                        LocaleContextHolder.resetLocaleContext();
                        RequestContextHolder.resetRequestAttributes();
                    }
                    MDC.clear();
                }
            };
        }


        @Override
        public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixProperty<Integer> corePoolSize,
                                                HystrixProperty<Integer> maximumPoolSize, HystrixProperty<Integer> keepAliveTime,
                                                TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            return originalStrategy.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }

        @Override
        public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolProperties threadPoolProperties) {
            return originalStrategy.getThreadPool(threadPoolKey, threadPoolProperties);
        }

        @Override
        public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
            return originalStrategy.getBlockingQueue(maxQueueSize);
        }

        @Override
        public <T> HystrixRequestVariable<T> getRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
            return originalStrategy.getRequestVariable(rv);
        }
    }
}
