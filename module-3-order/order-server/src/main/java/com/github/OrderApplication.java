package com.github;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@EnableDiscoveryClient
public class OrderApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(OrderApplication.class);
    }

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(OrderApplication.class, args);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
            if (A.isNotEmpty(activeProfiles)) {
                LogUtil.ROOT_LOG.debug("current profile : ({})", A.toStr(activeProfiles));
            }
        }
    }
}
