package com.github.common.exception;

import java.io.Serializable;

/** 没有权限 */
public class ForbiddenException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public ForbiddenException(String msg) {
        super(msg);
    }
    public ForbiddenException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public Throwable fillInStackTrace() { return this; }
}
