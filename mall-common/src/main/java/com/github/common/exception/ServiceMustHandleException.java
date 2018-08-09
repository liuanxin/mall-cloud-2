package com.github.common.exception;

import java.io.Serializable;

/** 业务异常. 在 Service 层抛出, 后台渲染页面时需要在 service 中 catch 并将异常信息返回到前台页面 */
public class ServiceMustHandleException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;

    public ServiceMustHandleException() {super();}
    public ServiceMustHandleException(String msg) {super(msg);}

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}
