package com.github.task;

import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

/** 动态设置运行时间的定时任务 --> 示例 */
// @Component
public class DynamicCronTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "下架商品";
    /** 当前任务的默认表达式 */
    private static final String CRON = "0 0 0/1 * * *";

    // @Autowired
    // private ProductService productService;

    // @Autowired
    // private CommonService commonService;

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
                        LogUtil.ROOT_LOG.error(BUSINESS_DESC + "异常", e);
                    }
                } finally {
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        LogUtil.ROOT_LOG.info(BUSINESS_DESC + "完成");
                    }
                    LogUtil.unbind();
                }
            }
        };

        Trigger trigger = new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                // 从数据库读取 cron 表达式
                String cron = ""; // commonService.getAbcCron();
                if (U.isBlank(cron)) {
                    // 如果没有, 给一个默认值.
                    cron = CRON;
                }

                // 如果设置的表达式有误也使用默认的
                CronTrigger cronTrigger;
                try {
                    cronTrigger = new CronTrigger(cron);
                } catch (Exception e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error(String.format("%s的表达式有误, 使用默认值(%s)", BUSINESS_DESC, CRON), e);
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

