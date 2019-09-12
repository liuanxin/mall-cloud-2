package com.github.common.annotation;

import java.lang.annotation.*;

/**
 * <pre>
 * <span style="color:red">当前注解主要用在 B 类网站上 --> 标注后的接口不登录也能访问</span>
 *
 * A 类网站: 大多数接口都是不需要登录的, 只有一些请求需要验证登录, 比如 weibo taobao 等
 *    在需要验证登录的接口上标 {@link NeedLogin}, 如 下单、支付...
 *
 * B 类网站: 大多数接口都是需要登录的, 只有很少的请求不需要验证登录, 比如管理系统
 *    在不需要验证登录的接口上标 {@link NotNeedLogin}, 如 登录...
 *    在需要验证登录但是不需要验证权限的接口标 {@link NotNeedPermission}, 如 查询个人信息...
 *
 * 方法上标了则以方法上为准
 * 如果类上标了 @NotNeedLogin, 里面一个具体的接口上标了 @NotNeedLogin(false) 则表示这个接口依然需要验证登录
 * </pre>
 */

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotNeedLogin {

    boolean value() default true;
}
