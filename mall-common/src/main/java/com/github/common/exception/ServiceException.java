package com.github.common.exception;

import java.io.Serializable;

/** 业务异常 */
public class ServiceException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceException(String msg) {
        super(msg);
    }
    public ServiceException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public Throwable fillInStackTrace() { return this; }
}
