package com.github.common.annotation;

import java.lang.annotation.*;

/**
 * 在不需要验证用户是否已登录的方法或类上标注即可(后台通常都是需要登录的)<br>
 * <span style="color:red;">优先级高于 NotNeedPermission</span>, 也就是如果两个注解都有标注时, 对应类或方法的 url 就不需要登录
 * <br><br>
 *
 * 在 Controller 类上标注后, 这个类下面的方法都不会再验证登录.<br>
 * 想要在某个方法上再次验证登录, 可以在这个方法上再标注一下这个注解并设置 flag = false
 *
 * @see NotNeedPermission
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotNeedLogin {

    boolean value() default true;
}
