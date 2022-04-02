package com.github.common.exception;

import org.springframework.http.ResponseEntity;

import java.io.Serializable;

/** 业务异常 */
public class ForceReturnException extends RuntimeException implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResponseEntity<?> response;

    public ForceReturnException(ResponseEntity<?> response) {
        this.response = response;
    }

    public ResponseEntity<?> getResponse() {
        return response;
    }
}
