package com.github.global.config;

import com.github.common.Const;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Sets;
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
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@ConditionalOnClass({HttpServletRequest.class, FeignClient.class, Feign.class})
public class FeignConfig {

    private static final Set<String> IGNORE_HEADER_SET = Sets.newHashSet("content-length", "accept");

    /** 处理请求头: 把请求上下文的头放到 Feign 的请求上下文中去 */
    @Bean
    public RequestInterceptor handleHeader() {
        return template -> {
            // 将当前请求上下文的 header 的信息放到请求 feign 的 header 中去
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                Enumeration<String> headers = request.getHeaderNames();
                while (headers.hasMoreElements()) {
                    String headName = headers.nextElement();
                    if (!IGNORE_HEADER_SET.contains(headName.toLowerCase())) {
                        template.header(headName, request.getHeader(headName));
                    }
                }

                // 将跟踪号放到请求上下文
                String traceId = String.valueOf(request.getAttribute(Const.TRACE));
                if (U.isNotEmpty(traceId)) {
                    template.header(Const.TRACE, traceId);
                }
            }
        };
    }

    /**
     * 处理日志打印: 需要配置 feign.client.config.default.loggerLevel 的值才能进入下面的日志打印,
     * 这个值默认是 NONE, 只要不是 NONE 就行(可以设置为 BASIC、HEADERS、FULL),
     * 默认的日志打印会打印出很多条, 见 {@link Logger},
     * 当前处理是只在请求前打印一条, 有响应时打印一条, io 异常时打印一条
     */
    @Bean
    public Logger handleLog() {
        return new Logger() {
            @Override
            protected void log(String configKey, String format, Object... args) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("feignClient --> " + methodTag(configKey) + "-> " + format);
                }
            }

            @Override
            protected void logRequest(String configKey, Level logLevel, Request request) {
                StringBuilder sbd = new StringBuilder("req:[");
                sbd.append("header(");
                for (Map.Entry<String, Collection<String>> entry : request.headers().entrySet()) {
                    sbd.append("<").append(entry.getKey()).append(" : ").append(entry.getValue()).append(">");
                }
                sbd.append(")");

                byte[] body = request.body();
                if (body != null && body.length > 0) {
                    String data = request.isBinary() ? "Binary data" : new String(body, request.charset());
                    sbd.append(" body(").append(data).append(")");
                }
                sbd.append("]");
                log(configKey, sbd.toString());
            }

            @Override
            protected Response logAndRebufferResponse(String configKey, Level level, Response response, long useTime) {
                StringBuilder sbd = new StringBuilder("res:[");
                sbd.append("time(").append(useTime).append(" ms) ").append(response.status());
                if (U.isNotBlank(response.reason())) {
                    sbd.append(' ').append(response.reason());
                }
                if (response.body() != null && response.body().isRepeatable()) {
                    sbd.append(" return(");
                    try (Reader reader = response.body().asReader(StandardCharsets.UTF_8)) {
                        sbd.append(CharStreams.toString(reader));
                    } catch (Exception ignore) {
                    }
                    sbd.append(")");
                }
                sbd.append("]");
                log(configKey, sbd.toString());
                return response;
            }

            @Override
            protected IOException logIOException(String configKey, Level logLevel, IOException e, long useTime) {
                StringBuilder sbd = new StringBuilder("error:[");
                sbd.append("time(").append(useTime).append(" ms) ");
                sbd.append(e.getClass().getSimpleName()).append(":").append(e.getMessage());
                sbd.append("]");
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("feignClient --> " + methodTag(configKey) + "-> " + sbd, e);
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
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            // 把主线程运行时的日志上下文放到 feign 的日志上下文去
            return () -> {
                try {
                    MDC.setContextMap(contextMap);
                    return callable.call();
                } finally {
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
