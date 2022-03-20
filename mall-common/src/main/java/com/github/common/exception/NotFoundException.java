package com.github.common.exception;

import java.io.Serializable;

public class NotFoundException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    public NotFoundException(String msg) {
        super(msg);
    }
    public NotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    @Override
    public Throwable fillInStackTrace() { return this; }
}
