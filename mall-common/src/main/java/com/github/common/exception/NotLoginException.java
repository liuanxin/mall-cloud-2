package com.github.common.exception;

import java.io.Serializable;

/** 用户未登录的统一处理 */
public class NotLoginException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public NotLoginException() {
        super("请先登录");
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
