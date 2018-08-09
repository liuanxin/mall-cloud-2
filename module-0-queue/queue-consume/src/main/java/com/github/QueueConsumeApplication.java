package com.github;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.annotation.EnableJms;

@EnableJms
@EnableFeignClients
@SpringBootApplication
@EnableDiscoveryClient
public class QueueConsumeApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = new SpringApplicationBuilder(QueueConsumeApplication.class)
                .web(WebApplicationType.NONE).run(args);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
            if (A.isNotEmpty(activeProfiles)) {
                LogUtil.ROOT_LOG.debug("current profile : ({})", A.toStr(activeProfiles));
            }
        }
    }
}
