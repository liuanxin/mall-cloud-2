package com.github.task;

import com.github.common.date.DateUtil;
import com.github.common.service.CommonService;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.github.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/** 动态设置运行时间的定时任务 --> 示例 */
@Component
@RequiredArgsConstructor
public class DynamicCronTask implements SchedulingConfigurer {

    /** 当前定时任务的业务说明 */
    private static final String BUSINESS_DESC = "下架商品";
    /** 当前任务的默认表达式 */
    private static final String CRON = "0 0 0/1 * * *";

    private final ProductService productService;
    private final CommonService commonService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        Runnable task = () -> {
            long start = System.currentTimeMillis();
            try {
                LogUtil.bindBasicInfo(U.uuid16());
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("{}开始", BUSINESS_DESC);
                }
                handlerBusiness();
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error(BUSINESS_DESC + "异常", e);
                }
            } finally {
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    LogUtil.ROOT_LOG.info("处理 job 结束({}), 耗时: ({})", BUSINESS_DESC, DateUtil.toHuman(System.currentTimeMillis() - start));
                }
                LogUtil.unbind();
            }
        };

        Trigger trigger = (triggerContext) -> {
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
                    LogUtil.ROOT_LOG.error("{}的表达式有误, 使用默认值({})", BUSINESS_DESC, CRON, e);
                }
                cronTrigger = new CronTrigger(CRON);
            }
            return cronTrigger.nextExecutionTime(triggerContext);
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

