//package com.github.global.config;
//
//import com.google.common.collect.Sets;
//import feign.Feign;
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
//import org.springframework.cloud.openfeign.FeignClient;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.context.request.RequestAttributes;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.Enumeration;
//import java.util.Set;
//
//@Configuration
//@ConditionalOnClass({HttpServletRequest.class, FeignClient.class, Feign.class})
//public class FeignInterceptor implements RequestInterceptor {
//
//    private static final Set<String> IGNORE_HEADER_SET = Sets.newHashSet(
//            "content-length", "content-type", "accept"
//    );
//
//    @Override
//    public void apply(RequestTemplate template) {
//        // 在项目拦截器中需要设置子线程共用
//        // RequestContextHolder.setRequestAttributes(RequestContextHolder.getRequestAttributes(), true);
//        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
//        if (requestAttributes instanceof ServletRequestAttributes) {
//            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//            Enumeration<String> headers = request.getHeaderNames();
//            while (headers.hasMoreElements()) {
//                String headName = headers.nextElement();
//                if (!IGNORE_HEADER_SET.contains(headName.toLowerCase())) {
//                    template.header(headName, request.getHeader(headName));
//                }
//            }
//            // template.header(Const.TRACE, RequestUtils.getCookieOrHeaderOrParam(Const.TRACE));
//        }
//    }
//}
