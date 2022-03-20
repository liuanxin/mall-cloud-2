package com.github.common.exception;

import java.io.Serializable;

/** 错误的请求异常 */
public class BadRequestException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public BadRequestException(String msg) {
        super(msg);
    }
    public BadRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public Throwable fillInStackTrace() { return this; }
}
