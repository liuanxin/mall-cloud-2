package com.github.common.annotation;

import java.lang.annotation.*;

/**
 * 在不需要验证用户是否有权限的类或方法上标注即可(后台通常都是需要权限的)<br/>
 * 当某些 url 只需要用户登录后就可以访问时, 标注此注解<br/>
 * <span style="color:red">优先级低于 NotNeedLogin</span>, 也就是如果两个注解都有标注时, 对应类或方法的 url 就不需要登录
 * <br><br>
 *
 * 在 Controller 类上标注后, 这个类下面的方法都不会再验证权限.<br>
 * 想要在某个方法上再次验证权限, 可以在这个方法上再标注一下这个注解并设置 flag = false
 *
 * @see NotNeedLogin
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NotNeedPermission {

    boolean flag() default true;
}
