package com.github.common.mvc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.github.common.converter.StringToDateConverter;
import com.github.common.converter.StringToEnumConverter;
import com.github.common.converter.StringToMoneyConverter;
import com.github.common.converter.StringToNumberConverter;
import com.github.common.json.JsonUtil;
import com.github.common.page.Page;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import org.springframework.core.MethodParameter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class SpringMvc {

    public static void handlerFormatter(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToNumberConverter());
        registry.addConverterFactory(new StringToEnumConverter());
        registry.addConverter(new StringToDateConverter());
        registry.addConverter(new StringToMoneyConverter());
    }

    public static void handlerConvert(List<HttpMessageConverter<?>> converters) {
        handlerStringConvert(converters, StringHttpMessageConverter.class, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        handlerStringConvert(converters, MappingJackson2HttpMessageConverter.class, new CustomizeJacksonConverter());
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
    public static class CustomizeJacksonConverter extends MappingJackson2HttpMessageConverter {
        @Override
        protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
            super.writeSuffix(generator, object);

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String json = JsonUtil.toJsonNil(object);
                if (U.isNotBlank(json)) {
                    boolean notRequestInfo = LogUtil.hasNotRequestInfo();
                    try {
                        if (notRequestInfo) {
                            LogUtil.bind(RequestUtils.logContextInfo());
                        }
                        // 太长就只输出前后, 不全部输出
                        LogUtil.ROOT_LOG.info("return: ({})", U.toStr(json, 1000, 200));
                    } finally {
                        if (notRequestInfo) {
                            LogUtil.unbind();
                        }
                    }
                }
            }
        }
    }

    public static void handlerArgument(List<HandlerMethodArgumentResolver> argumentResolvers) {
        // 参数是 Page 对象时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Page.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                Page page = new Page(request.getParameter(Page.GLOBAL_PAGE), request.getParameter(Page.GLOBAL_LIMIT));
                page.setWasMobile(RequestUtils.isMobileRequest());
                return page;
            }
        });
        // 参数是 page 名称时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Page.GLOBAL_PAGE.equals(parameter.getParameterName());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                return Page.handlerPage(request.getParameter(Page.GLOBAL_PAGE));
            }
        });
        // 参数是 limit 名称时
        argumentResolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return Page.GLOBAL_LIMIT.equals(parameter.getParameterName());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest request, WebDataBinderFactory factory) throws Exception {
                return Page.handlerLimit(request.getParameter(Page.GLOBAL_LIMIT));
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
