package com.github.common.exception;

import java.io.Serializable;

/** 未登录 */
public class NotLoginException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public NotLoginException() {
        super("请先登录");
    }
    public NotLoginException(String msg) {
        super(msg);
    }
    public NotLoginException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public Throwable fillInStackTrace() { return this; }
}
