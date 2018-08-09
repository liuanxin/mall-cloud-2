package com.github.global.config;

import com.github.common.util.ApplicationContexts;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GlobalConfig {

    @Bean
    public ApplicationContexts setupApplicationContext() {
        return new ApplicationContexts();
    }
}
