package com.github.search.service;

import com.github.common.util.LogUtil;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DataSyncService implements SchedulingConfigurer {

    static final String CRON = "0 * * * * *";

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                LogUtil.recordTime();
                try {
                    handlerBusiness();
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("同步数据到搜索时异常", e);
                    }
                } finally {
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info("同步数据到搜索完成");
                    }
                    LogUtil.unbind();
                }
            }
        };

        Trigger trigger = new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                String cron = "";
                // 如果设置的表达式有误也使用默认的
                CronTrigger cronTrigger;
                try {
                    cronTrigger = new CronTrigger(cron);
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error(String.format("配置的表达式(%s)有误, 使用默认", cron), e);
                    }
                    cronTrigger = new CronTrigger(CRON);
                }
                return cronTrigger.nextExecutionTime(triggerContext);
            }
        };
        taskRegistrar.addTriggerTask(task, trigger);
    }

    /** 操作具体的业务 */
    private void handlerBusiness() {
        // int offlineCount = productService.yyy();
        // if (LogUtil.ROOT_LOG.isInfoEnabled()) {
        //     LogUtil.ROOT_LOG.info("{}时共操作了 {} 个商品", BUSINESS_DESC, offlineCount);
        // }
    }
}

