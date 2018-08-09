package com.github.config;

import com.github.common.mvc.SpringMvc;
import com.github.common.mvc.VersionRequestMappingHandlerMapping;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;

/**
 * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
 * @see WebMvcConfigurationSupport
 * @see org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.WebMvcAutoConfigurationAdapter
 */
@Configuration
public class BackendWebConfig extends WebMvcConfigurationSupport {

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new VersionRequestMappingHandlerMapping();
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        SpringMvc.handlerFormatter(registry);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        SpringMvc.handlerConvert(converters);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        SpringMvc.handlerArgument(argumentResolvers);
    }

    /**
     * 这种方式下方法上将不能标 @ResponseBody,
     * 标了的话会被 {@link org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor} 处理.
     *
     * @see com.github.config.BackendJsonResultAdvice
     */
    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        // SpringMvc.handlerReturn(returnValueHandlers);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BackendInterceptor()).addPathPatterns("/**");
    }
}
