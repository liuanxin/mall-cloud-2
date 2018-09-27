package com.github.common.mvc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.smmdhao.common.converter.StringToDateConverter;
import com.smmdhao.common.converter.StringToEnumConverter;
import com.smmdhao.common.converter.StringToMoneyConverter;
import com.smmdhao.common.converter.StringToNumberConverter;
import com.smmdhao.common.json.JsonUtil;
import com.smmdhao.common.page.Page;
import com.smmdhao.common.util.A;
import com.smmdhao.common.util.LogUtil;
import com.smmdhao.common.util.RequestUtils;
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

public final class SpringMvc {

    public static void handlerFormatter(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToNumberConverter());
        registry.addConverterFactory(new StringToEnumConverter());
        registry.addConverter(new StringToDateConverter());
        registry.addConverter(new StringToMoneyConverter());
    }

    public static void handlerConvert(List<HttpMessageConverter<?>> converters) {
        int i = 0, json = 0, string = 0;
        if (A.isNotEmpty(converters)) {
            Iterator<HttpMessageConverter<?>> iterator = converters.iterator();
            while (iterator.hasNext()) {
                HttpMessageConverter<?> converter = iterator.next();
                if (converter instanceof MappingJackson2HttpMessageConverter) {
                    iterator.remove();
                    json = i;
                }
                if (converter instanceof StringHttpMessageConverter) {
                    iterator.remove();
                    string = i;
                }
                i++;
            }
        }
        // 放到它们原来在的位置
        if (string > json) {
            converters.add(json, new CustomizeJacksonConverter());
            converters.add(string, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        } else {
            converters.add(string, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            converters.add(json, new CustomizeJacksonConverter());
        }
    }

    public static class CustomizeJacksonConverter extends MappingJackson2HttpMessageConverter {
        CustomizeJacksonConverter() { super(JsonUtil.RENDER); }
        @Override
        protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
            super.writeSuffix(generator, object);

            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                String toRender = JsonUtil.toJson(object);
                // 如果长度大于 6000 就只输出前 200 个字符
                if (toRender.length() > 6000) {
                    toRender = toRender.substring(0, 200) + " ...";
                }
                LogUtil.ROOT_LOG.info("return json: ({})", toRender);
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
