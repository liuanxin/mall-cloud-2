package com.github.global.config;

import com.github.common.util.U;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务用到的配置, 如果有多个 @Scheduled 注解时默认是单线程, 任务将会是串行执行的, 且上次没有执行完不会执行下次.
 * 此配置将表示为多线程, 任务将会并行执行, 且上次没有执行完不会执行下次
 */
@Configuration
public class ScheduleConfig {

    @Bean
    public ThreadPoolTaskScheduler scheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(U.PROCESSORS << 2);
        scheduler.setThreadNamePrefix("scheduler-");
        return scheduler;
    }
}
