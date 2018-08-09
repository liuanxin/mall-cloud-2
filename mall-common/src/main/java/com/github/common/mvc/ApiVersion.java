package com.github.common.mvc;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

/** 在需要对 url 做多版本的时候标注, 可以在类上, 也可以在方法上. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface ApiVersion {

    /**
     * 标注的方法版本, 将会从小到大做兼容.<br>
     * 比如有 v v1、v5 三个版本的代码, 其中 v 表示不带版本.<br>
     * 当用户使用 v2 v3 v4 的版本请求时, 也将进到 v1 里面去
     */
    AppVersion value();
}
