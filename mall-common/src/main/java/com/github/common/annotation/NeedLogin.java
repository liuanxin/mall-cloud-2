package com.github.common.annotation;

import java.lang.annotation.*;

/**
 * 在需要验证用户是否已登录的方法或类上标注即可(用户中心通常都是需要登录的)<br><br>
 *
 * 在 Controller 类上标注后, 这个类下面的方法都会验证登录.<br>
 * 想要在某个方法上放过, 可以在这个方法上再标注一下这个注解并设置 flag = false
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NeedLogin {

    boolean value() default true;
}
