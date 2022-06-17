package com.github.global.config;

import com.github.common.util.AsyncUtil;
import com.github.common.util.U;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 让 @Async 注解下的方法异步处理的线程池. 下面的代码相当于以下的配置<br><br>
 * &lt;task:annotation-driven executor="myExecutor" exception-handler="exceptionHandler"/&gt;<br>
 * &lt;task:executor id="myExecutor" pool-size="4-32" queue-capacity="8"/&gt;<br>
 * &lt;context id="exceptionHandler" class="...SimpleAsyncUncaughtExceptionHandler"/&gt;<br><br>
 *
 * 如果想异步处理后返回结果, 在 @Async 注解的方法返回上使用 Future<>, 方法体里面的返回使用 new AsyncResult<>()
 */
@Configuration
@EnableAsync
public class TaskConfig implements AsyncConfigurer {

    /**
     * @see org.springframework.boot.autoconfigure.task.TaskExecutionProperties
     * @see org.springframework.aop.interceptor.AsyncExecutionInterceptor#determineAsyncExecutor
     */
    @SuppressWarnings({"JavadocReference", "NullableProblems"})
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 如果 cpu 核心是  2, 则最前面的  2 个异步处理, 接下来的  496 个放进队列, 之后的 2 个( 4 -  2)异步处理, 再往后的拒绝
        // 如果 cpu 核心是  4, 则最前面的  4 个异步处理, 接下来的  992 个放进队列, 之后的 2 个( 6 -  4)异步处理, 再往后的拒绝
        // 如果 cpu 核心是  8, 则最前面的  8 个异步处理, 接下来的 1984 个放进队列, 之后的 2 个(10 -  8)异步处理, 再往后的拒绝
        // 如果 cpu 核心是 16, 则最前面的 16 个异步处理, 接下来的 3968 个放进队列, 之后的 2 个(18 - 16)异步处理, 再往后的拒绝
        // 如果 cpu 核心是 32, 则最前面的 32 个异步处理, 接下来的 7936 个放进队列, 之后的 2 个(34 - 32)异步处理, 再往后的拒绝
        executor.setCorePoolSize(U.PROCESSORS);
        executor.setMaxPoolSize(U.PROCESSORS + 2);
        executor.setQueueCapacity((U.PROCESSORS << 8) - (U.PROCESSORS << 3));
        executor.setThreadNamePrefix("task-executor-");
        // 见: https://moelholm.com/blog/2017/07/24/spring-43-using-a-taskdecorator-to-copy-mdc-data-to-async-threads
        executor.setTaskDecorator(AsyncUtil::wrapRunContext);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }
}
