package com.github.global.config;

import com.github.common.mvc.CorsFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.servlet.Filter;

@Configuration
@ConditionalOnClass({ Filter.class })
public class GlobalWebConfig {

    @Value("${http.cors.allow-headers:}")
    private String allowHeaders;

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterFilter(CharacterEncodingFilter filter) {
        FilterRegistrationBean<CharacterEncodingFilter> filterBean = new FilterRegistrationBean<>(filter);
        filterBean.setOrder(Integer.MIN_VALUE);
        return filterBean;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        FilterRegistrationBean<CorsFilter> filterBean = new FilterRegistrationBean<>(new CorsFilter(allowHeaders));
        filterBean.setOrder(Integer.MIN_VALUE + 1);
        return filterBean;
    }
}
