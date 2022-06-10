package com.github.global.config;

import com.github.common.Const;
import com.github.common.util.*;
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
import feign.codec.Decoder;
import feign.optionals.OptionalDecoder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 处理 feign 的请求头、日志打印、MDC 上下文
 *
 * 使用 @FeignClient 时, 用 fallbackFactory 属性, 实现 {@link feign.hystrix.FallbackFactory} 并打印 Throwable,
 * 如果用 fallback 属性, 实现类没有异常信息, 出错将无法输出日志
 *
 * @see org.springframework.cloud.openfeign.FeignAutoConfiguration
 * @see org.springframework.cloud.openfeign.FeignClientsConfiguration
 */
@RequiredArgsConstructor
@Configuration
@ConditionalOnClass({ FeignClient.class, Feign.class })
public class FeignConfig {

    /** 要放到 feign 请求头里的键, 如果将全部的请求头都放进 feign 请求头, 可能会覆盖 Content-Type 的值 */
    private static final Set<String> HEADER_SET = Sets.newHashSet(
            Const.TOKEN.toLowerCase(), "cookie", "user-agent", "accept-language", "x-real-ip"
    );

    private final JsonDesensitization jsonDesensitization;

    /** 处理请求头: 把请求信息放到 Feign 的请求上下文中去(feign 默认会放 Content-Length : xxx 和 Content-Type : application/json 到请求头里去) */
    @Bean
    @ConditionalOnClass(HttpServletRequest.class)
    public RequestInterceptor handleHeader() {
        return template -> {
            Map<String, Collection<String>> currentHeaders = template.headers();
            String traceId = null;
            // 将当前请求上下文的 header 信息放到请求 feign 的 header 中去
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

                Enumeration<String> headers = request.getHeaderNames();
                while (headers.hasMoreElements()) {
                    String headName = headers.nextElement();
                    if (HEADER_SET.contains(headName.toLowerCase())) {
                        String headerValue = request.getHeader(headName);
                        // 原头里不包括请求头的值就放进去
                        if (U.isNotEmpty(headerValue) && !currentHeaders.get(headName).contains(headerValue)) {
                            // 先清空再设置
                            template.header(headName, Collections.emptyList()).header(headName, headerValue);
                        }
                    }
                }
                // 从请求上获取跟踪号
                traceId = request.getHeader(Const.TRACE);
            }

            // 跟踪号如果为空则从日志获取跟踪号
            if (U.isEmpty(traceId)) {
                traceId = LogUtil.getTraceId();
            }
            if (U.isNotEmpty(traceId)) {
                if (A.isNotEmpty(currentHeaders)) {
                    // 头里面没有就放到头
                    if (!currentHeaders.get(Const.TRACE).contains(traceId)) {
                        template.header(Const.TRACE, Collections.emptyList()).header(Const.TRACE, traceId);
                    }
                } else {
                    template.header(Const.TRACE, Collections.emptyList()).header(Const.TRACE, traceId);
                }
            }
        };
    }

    /** @see org.springframework.cloud.openfeign.FeignClientsConfiguration#feignDecoder() */
    @Bean("feignDecoder")
    public Decoder selfDecoder(ObjectFactory<HttpMessageConverters> messageConverters) {
        return new OptionalDecoder(new ResponseEntityDecoder(new SpringDecoder(messageConverters) {
            @Override
            public Object decode(Response res, Type type) throws IOException, FeignException {
                try {
                    return super.decode(res, type);
                } catch (IOException | FeignException e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("feignClient decode exception", e);
                    }
                    throw e;
                }
            }
        }));
    }

    /**
     * 使用 ribbon 做负载均衡时用到
     *
     * @see org.springframework.cloud.openfeign.ribbon.DefaultFeignLoadBalancedConfiguration
     */
    @Bean("feignClient")
    @ConditionalOnBean(CachingSpringLoadBalancerFactory.class)
    @ConditionalOnProperty("spring.cloud.loadbalancer.ribbon.enabled")
    @SuppressWarnings({ "JavadocReference", "SpringJavaInjectionPointsAutowiringInspection" })
    public Client ribbonClient(CachingSpringLoadBalancerFactory cachingFactory, SpringClientFactory clientFactory) {
        return new LoadBalancerFeignClient(new SelfClient(), cachingFactory, clientFactory);
    }

    /**
     * 使用 feign 调用如果用的是 name 而不是 url 时, 日志只能输出在注册中心中用到的服务名, 负载均衡后将可以获取到具体的 ip:port.
     * 注意: 负载均衡, 如果用的是 ribbon 则用上面的配置, 如果用的是 loadbalancer 则用下面的配置
     */
    public static class SelfClient extends Client.Default {
        public SelfClient() {
            super(null, null);
        }
        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("feignClient method({}) real-url({})", request.httpMethod().name(), request.url());
            }
            return super.execute(request, options);
        }
    }

    /**
     * 使用 loadbalancer 做负载均衡时用到
     *
     * @see org.springframework.cloud.openfeign.loadbalancer.DefaultFeignLoadBalancerConfiguration
     */
    @Bean("feignClient")
    @SuppressWarnings("JavadocReference")
    @ConditionalOnBean(BlockingLoadBalancerClient.class)
    public Client loadbalancerClient(BlockingLoadBalancerClient loadBalancerClient) {
        return new FeignBlockingLoadBalancerClient(new SelfClient(), loadBalancerClient);
    }

    /**
     * 处理日志打印: 需要配置 feign.client.config.default.loggerLevel 的值才能进入下面的日志打印,
     * 这个值默认是 NONE, 只要不是 NONE 就行(可以设置为 BASIC、HEADERS、FULL),
     * 默认的日志会输出很多条, 见 {@link Logger}, 当前处理是只在请求前打印一条, 有响应时打印一条, io 异常时打印一条
     *
     * @see org.springframework.cloud.openfeign.DefaultFeignLoggerFactory
     */
    @Bean
    public Logger handleLog() {
        return new Logger() {
            @Override
            protected void log(String configKey, String format, Object... args) {}

            @Override
            protected void logRequest(String configKey, Level logLevel, Request request) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    StringBuilder sbd = new StringBuilder("request:[");
                    sbd.append(request.httpMethod().name()).append(" ").append(request.url());
                    collectHeader(sbd, request.headers());

                    byte[] body = request.body();
                    if (body != null && body.length > 0) {
                        String data = request.isBinary() ? "Binary data" : new String(body, request.charset());
                        sbd.append(" body(").append(jsonDesensitization.toJson(data)).append(")");
                    }
                    sbd.append("]");
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
            protected Response logAndRebufferResponse(String configKey, Level level, Response res, long useTime) {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    StringBuilder sbd = new StringBuilder("response:[");
                    int status = res.status();
                    sbd.append("time(").append(useTime).append(" ms), status(").append(status).append(")");
                    String reason = res.reason();
                    if (U.isNotBlank(reason)) {
                        sbd.append(", reason(").append(reason).append(")");
                    }
                    Map<String, Collection<String>> headers = res.headers();
                    collectHeader(sbd, headers);

                    String data = null;
                    Response.Body body = res.body();
                    if (U.isNotNull(body)) {
                        if (body.isRepeatable()) {
                            try (Reader reader = body.asReader(StandardCharsets.UTF_8)) {
                                data = CharStreams.toString(reader);
                            } catch (Exception e) {
                                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                    LogUtil.ROOT_LOG.error("feignClient <--> {} <-> body reader exception", methodTag(configKey), e);
                                }
                            }
                        } else {
                            try (InputStream inputStream = body.asInputStream()) {
                                byte[] bytes = ByteStreams.toByteArray(inputStream);
                                data = new String(bytes);
                                res = Response.builder().status(status).reason(reason)
                                        .headers(headers).request(res.request()).body(bytes).build();
                            } catch (Exception e) {
                                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                                    LogUtil.ROOT_LOG.error("feignClient <--> {} <-> body input-stream exception", methodTag(configKey), e);
                                }
                            }
                        }
                        if (U.isNotBlank(data)) {
                            sbd.append(", return(").append(jsonDesensitization.toJson(data)).append(")");
                        }
                    }
                    sbd.append("]");
                    LogUtil.ROOT_LOG.info("feignClient <-- {} <- {}", methodTag(configKey), sbd);
                }
                return res;
            }

            @Override
            protected IOException logIOException(String configKey, Level level, IOException e, long useTime) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    String clazzName = e.getClass().getSimpleName();
                    String msg = String.format("exception:[time(%sms) %s: %s]", useTime, clazzName, e.getMessage());
                    LogUtil.ROOT_LOG.error("feignClient <--> {} <-> {}", methodTag(configKey), msg, e);
                }
                return e;
            }

            private void collectHeader(StringBuilder sbd, Map<String, Collection<String>> headers) {
                if (A.isNotEmpty(headers)) {
                    sbd.append("header(");
                    for (Map.Entry<String, Collection<String>> entry : headers.entrySet()) {
                        sbd.append("<");
                        sbd.append(entry.getKey()).append(" : ").append(DesensitizationUtil.des(entry.getKey(), A.toStr(entry.getValue())));
                        sbd.append(">");
                    }
                    sbd.append(")");
                }
            }
        };
    }

    /** 处理上下文: 把主线程的请求和日志上下文放到 feign 的上下文去 */
    @Bean
    public HystrixConcurrencyStrategy handleContext() {
        return new ContextFeignConcurrencyStrategy();
    }

    public static class ContextFeignConcurrencyStrategy extends HystrixConcurrencyStrategy {
        private final HystrixConcurrencyStrategy originalStrategy;

        public ContextFeignConcurrencyStrategy() {
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
            return AsyncUti.wrapCall(callable);
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
