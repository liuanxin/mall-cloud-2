package com.github.global.config;

import com.github.common.exception.*;
import com.github.common.json.JsonCode;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;

/**
 * 处理全局异常的控制类
 *
 * @see org.springframework.boot.web.servlet.error.ErrorController
 * @see org.springframework.boot.autoconfigure.web.ErrorProperties
 * @see org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
 */
@ConditionalOnClass({ HttpServletRequest.class, ResponseEntity.class })
@RestControllerAdvice
public class GlobalException {

    @Value("${online:false}")
    private boolean online;

    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<String> service(ServiceException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("业务异常", e);
        }
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(e.getMessage());
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<String> notLogin(NotLoginException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没登录", e);
        }
        return ResponseEntity.status(JsonCode.NOT_LOGIN.getCode()).body(e.getMessage());
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> forbidden(ForbiddenException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没权限", e);
        }
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(e.getMessage());
        // return ResponseEntity.status(JsonCode.NOT_PERMISSION.getCode()).body(e.getMessage());
    }
    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> notFound(NotFoundException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("404", e);
        }
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(e.getMessage());
        // return ResponseEntity.status(JsonCode.NOT_FOUND.getCode()).body(e.getMessage());
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> badRequest(BadRequestException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("错误的请求", e);
        }
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(e.getMessage());
        // return ResponseEntity.status(JsonCode.BAD_REQUEST.getCode()).body(e.getMessage());
    }


    // 以下是 spring 的内部异常

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());

        bindAndPrintLog(msg, e);
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(msg);
        // return ResponseEntity.status(JsonCode.NOT_FOUND.getCode()).body(msg);
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> missParam(MissingServletRequestParameterException e) {
        String msg = online
                ? "无法响应此请求"
                : String.format("缺少必须的参数(%s), 类型(%s)", e.getParameterName(), e.getParameterType());

        bindAndPrintLog(msg, e);
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(msg);
        // return ResponseEntity.status(JsonCode.BAD_REQUEST.getCode()).body(msg);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online
                ? "无法处理此请求"
                : String.format("不支持此请求方式: 当前(%s), 支持(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));

        bindAndPrintLog(msg, e);
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(msg);
        // return ResponseEntity.status(JsonCode.BAD_REQUEST.getCode()).body(msg);
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("上传文件太大! 请保持在 %sM 以内", (e.getMaxUploadSize() >> 20));
        bindAndPrintLog(msg, e);
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(msg);
    }

    // 以上是 spring 的内部异常


    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> other(Throwable e) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("有错误", e);
        }

        Throwable cause = e.getCause();
        Throwable t = (cause == null ? e : cause);
        String msg = U.returnMsg(t, online);
        return ResponseEntity.status(JsonCode.FAIL.getCode()).body(msg);
    }

    // ==================================================

    private void bindAndPrintLog(String msg, Exception e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            // 当没有进到全局拦截器就抛出的异常, 需要这么处理才能在日志中输出整个上下文信息
            LogUtil.bind(RequestUtils.logContextInfo());
            try {
                LogUtil.ROOT_LOG.debug(msg, e);
            } finally {
                LogUtil.unbind();
            }
        }
    }
}
