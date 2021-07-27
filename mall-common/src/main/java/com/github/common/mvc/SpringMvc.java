package com.github.common.mvc;

import com.github.common.converter.*;
import com.github.common.page.PageParam;
import org.springframework.core.MethodParameter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class SpringMvc {

    public static void handlerFormatter(FormatterRegistry registry) {
        registry.addConverter(new String2BooleanConverter());
        registry.addConverterFactory(new StringToNumberConverter());
        registry.addConverterFactory(new StringToEnumConverter());
        registry.addConverter(new StringToDateConverter());
        registry.addConverter(new StringToMoneyConverter());
    }

    public static void handlerConvert(List<HttpMessageConverter<?>> converters) {
        handlerStringConvert(converters, StringHttpMessageConverter.class, new StringHttpMessageConverter(StandardCharsets.UTF_8));
    }
    private static void handlerStringConvert(List<HttpMessageConverter<?>> converters,
                                             Class<? extends HttpMessageConverter> clazz,
                                             HttpMessageConverter<?> httpMessageConverter) {
        Iterator<HttpMessageConverter<?>> iterator = converters.iterator();
        int i = 0;
        for (; iterator.hasNext(); i++) {
            HttpMessageConverter<?> converter = iterator.next();
            if (Objects.equals(converter.getClass(), clazz)) {
                iterator.remove();
                break;
            }
        }
        // 先删再加, 删的时候记下索引, 保证还在原来的位置
        converters.add(i, httpMessageConverter);
    }

    public static void handlerArgument(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 参数是 Page 对象时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return PageParam.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                // PageParam page = new PageParam(request.getParameter(PageParam.GLOBAL_PAGE), request.getParameter(PageParam.GLOBAL_LIMIT));
                // page.setWasMobile(RequestUtils.isMobileRequest());
                return new PageParam(request.getParameter(PageParam.GLOBAL_PAGE), request.getParameter(PageParam.GLOBAL_LIMIT));
            }
        });
        // 参数是 page 名称时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return PageParam.GLOBAL_PAGE.equals(parameter.getParameterName());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                return PageParam.handlerPage(request.getParameter(PageParam.GLOBAL_PAGE));
            }
        });
        // 参数是 limit 名称时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return PageParam.GLOBAL_LIMIT.equals(parameter.getParameterName());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                return PageParam.handlerLimit(request.getParameter(PageParam.GLOBAL_LIMIT));
            }
        });
    }

    /*
     * 这种方式下方法上将不能标 @ResponseBody,
     * 标了的话会被 {@link org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor} 处理.
     *
     * 见 com.github.config.BackendJsonResultAdvice 的处理方式
     *
    public static void handlerReturn(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
        // 当返回类型是 JsonResult 对象时
        returnValueHandlers.add(new HandlerMethodReturnValueHandler() {
            @Override
            public boolean supportsReturnType(MethodParameter parameter) {
                return JsonResult.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public void handleReturnValue(Object returnValue, MethodParameter returnType,
                                          ModelAndViewContainer container, NativeWebRequest request) throws Exception {
                container.setRequestHandled(true);
                String token = AppTokenHandler.resetTokenExpireTime();
                if (U.isNotBlank(token)) {
                    ((JsonResult) returnValue).setToken(token);
                }
            }
        });
    }
    */
}
