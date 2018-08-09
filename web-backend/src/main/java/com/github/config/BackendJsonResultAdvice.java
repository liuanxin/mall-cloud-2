package com.github.config;

import com.github.common.json.JsonResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Deprecated
//@ControllerAdvice
public class BackendJsonResultAdvice implements ResponseBodyAdvice<JsonResult> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return JsonResult.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public JsonResult beforeBodyWrite(JsonResult body, MethodParameter returnType, MediaType selectedContentType,
                                      Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                      ServerHttpRequest request, ServerHttpResponse response) {
        // 刷新一下 JsonResult 中 token 对应的超时时间
        /*String token = AppTokenHandler.resetTokenExpireTime();
        if (U.isNotBlank(token)) {
            body.setToken(token);
        }*/
        return body;
    }
}
