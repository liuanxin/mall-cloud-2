package com.github;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;

@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class WebManagerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(WebManagerApplication.class);
    }

    public static void main(String[] args) {
        ApplicationContext ctx = new SpringApplicationBuilder().sources(WebManagerApplication.class).run(args);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();
            if (A.isNotEmpty(activeProfiles)) {
                LogUtil.ROOT_LOG.debug("current profile : ({})", A.toStr(activeProfiles));
            }
        }
    }
}
