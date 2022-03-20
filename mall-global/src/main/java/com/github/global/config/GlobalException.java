package com.github.global.config;

import com.github.common.Const;
import com.github.common.exception.*;
import com.github.common.json.JsonCode;
import com.github.common.json.JsonResult;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtil;
import com.github.common.util.U;
import com.github.global.util.ValidationUtil;
import com.google.common.base.Joiner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    /**
     * 响应错误时, 错误码是否以 ResponseStatus 返回
     *
     * true:  ResponseStatus 返回 400 | 500, 返回 json 是 { "code": 400 | 500 ... }
     * false: ResponseStatus 返回 200,       返回 json 是 { "code": 400 | 500 ... }
     */
    @Value("${res.returnStatusCode:false}")
    private boolean returnStatusCode;

    /** 业务异常 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<JsonResult<Void>> service(ServiceException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("业务异常", e);
        }
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(e.getMessage()));
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<JsonResult<Void>> notLogin(NotLoginException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没登录", e);
        }
        int status = returnStatusCode ? JsonCode.NOT_LOGIN.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.needLogin(e.getMessage()));
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<JsonResult<Void>> forbidden(ForbiddenException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("没权限", e);
        }
        int status = returnStatusCode ? JsonCode.NOT_PERMISSION.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.needPermission(e.getMessage()));
    }
    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<JsonResult<Void>> notFound(NotFoundException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("404", e);
        }
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.notFound(e.getMessage()));
    }
    /** 参数验证 */
    @ExceptionHandler(ParamException.class)
    public ResponseEntity<JsonResult<Void>> param(ParamException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("参数验证不过", e);
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(e.getMessage(), e.getErrorMap()));
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<JsonResult<Void>> badRequest(BadRequestException e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("错误的请求", e);
        }
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(e.getMessage()));
    }


    // 以下是 spring 的内部异常

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult<Void>> noHandler(NoHandlerFoundException e) {
        String msg = online ? "404" : String.format("404(%s %s)", e.getHttpMethod(), e.getRequestURL());

        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.NOT_FOUND.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.notFound(msg));
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<JsonResult<Void>> missParam(MissingServletRequestParameterException e) {
        String msg = online ? "缺少必须的参数"
                : String.format("缺少必须的参数(%s), 类型(%s)", e.getParameterName(), e.getParameterType());

        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(msg));
    }
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<JsonResult<Void>> missHeader(MissingRequestHeaderException e) {
        String msg = online ? "缺少必须的信息" : String.format("缺少头(%s)", e.getHeaderName());

        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(msg));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<JsonResult<String>> paramValidException(MethodArgumentNotValidException e) {
        Map<String, String> errorMap = ValidationUtil.validate(e.getBindingResult());
        bindAndPrintLog(JsonUtil.toJson(errorMap), e);
        int status = returnStatusCode ? JsonCode.BAD_REQUEST.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.badRequest(Joiner.on(",").join(errorMap.values()), errorMap));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult<Void>> notSupported(HttpRequestMethodNotSupportedException e) {
        String msg = online ? "不支持此种方式"
                : String.format("不支持此种方式: 当前(%s), 支持(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(msg));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<JsonResult<Void>> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("上传文件太大! 请保持在 %sM 以内", (e.getMaxUploadSize() >> 20));
        bindAndPrintLog(msg, e);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(msg));
    }

    // 以上是 spring 的内部异常


    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<JsonResult<Void>> other(Throwable e) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("有错误", e);
        }

        Throwable cause = e.getCause();
        Throwable t = (cause == null ? e : cause);
        int status = returnStatusCode ? JsonCode.FAIL.getCode() : JsonCode.SUCCESS.getCode();
        return ResponseEntity.status(status).body(JsonResult.fail(U.returnMsg(t, online), errorTrack(e)));
    }

    // ==================================================

    private void bindAndPrintLog(String msg, Exception e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            // 当没有进到全局拦截器就抛出的异常, 需要这么处理才能在日志中输出整个上下文信息
            boolean notRequestInfo = LogUtil.hasNotRequestInfo();
            try {
                if (notRequestInfo) {
                    String traceId = RequestUtil.getCookieOrHeaderOrParam(Const.TRACE);
                    LogUtil.putContext(traceId, RequestUtil.logContextInfo());
                    LogUtil.putIp(RequestUtil.getRealIp());
                }
                LogUtil.ROOT_LOG.debug(msg, e);
            } finally {
                if (notRequestInfo) {
                    LogUtil.unbind();
                }
            }
        }
    }
    private List<String> errorTrack(Throwable e) {
        if (online) {
            return null;
        }

        List<String> errorList = new ArrayList<>();
        errorList.add(e.getMessage());
        for (StackTraceElement trace : e.getStackTrace()) {
            errorList.add(trace.toString());
        }
        return errorList;
    }
}
