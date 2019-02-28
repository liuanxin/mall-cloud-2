package com.github.global.config;

import com.github.common.exception.*;
import com.github.common.json.JsonResult;
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
        String msg = e.getMessage();
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug(msg);
        }

        JsonResult result = JsonResult.serviceFail(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    /** 未登录 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<String> notLogin(NotLoginException e) {
        String msg = e.getMessage();
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug(msg);
        }

        JsonResult result = JsonResult.notLogin(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    /** 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> forbidden(ForbiddenException e) {
        String msg = e.getMessage();
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug(msg);
        }

        JsonResult result = JsonResult.notPermission(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    /** 404 */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> notFound(NotFoundException e) {
        String msg = e.getMessage();
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug(msg);
        }

        JsonResult result = JsonResult.notFound(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    /** 错误的请求 */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> badRequest(BadRequestException e) {
        String msg = e.getMessage();
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug(msg);
        }

        JsonResult result = JsonResult.badRequest(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }


    // 以下是 spring 的内部异常

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> noHandler(NoHandlerFoundException e) {
        bindAndPrintLog(e);

        String msg = String.format("没找到(%s %s)", e.getHttpMethod(), e.getRequestURL());
        JsonResult result = JsonResult.notFound(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> missParam(MissingServletRequestParameterException e) {
        bindAndPrintLog(e);

        String msg = String.format("缺少必须的参数(%s), 类型(%s)", e.getParameterName(), e.getParameterType());
        JsonResult result = JsonResult.badRequest(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> notSupported(HttpRequestMethodNotSupportedException e) {
        bindAndPrintLog(e);

        String msg = "不支持此种请求方式.";
        if (!online) {
            msg += String.format(" 当前(%s), 支持(%s)", e.getMethod(), A.toStr(e.getSupportedMethods()));
        }
        JsonResult result = JsonResult.serviceFail(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> uploadSizeExceeded(MaxUploadSizeExceededException e) {
        bindAndPrintLog(e);

        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb
        String msg = String.format("上传文件太大! 请保持在 %sM 以内", (e.getMaxUploadSize() >> 20));
        JsonResult result = JsonResult.serviceFail(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }

    // 以上是 spring 的内部异常


    /** 未知的所有其他异常 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<String> other(Throwable e) {
        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
            LogUtil.ROOT_LOG.error("有错误", e);
        }

        String msg = U.returnMsg(e, online);
        JsonResult<Object> result = JsonResult.fail(msg);
        return ResponseEntity.status(result.getCode()).body(result.getMsg());
    }

    // ==================================================

    private void bindAndPrintLog(Exception e) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            // 当没有进到全局拦截器就抛出的异常, 需要这么处理才能在日志中输出整个上下文信息
            LogUtil.bind(RequestUtils.logContextInfo());
            try {
                LogUtil.ROOT_LOG.debug(e.getMessage(), e);
            } finally {
                LogUtil.unbind();
            }
        }
    }
}
