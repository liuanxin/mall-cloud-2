package com.github.common.util;

import com.github.common.date.DateUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Date;

/** 日志管理, 使用此 utils 获取 log, 不要在类中使用 LoggerFactory.getLogger 的方式! */
public final class LogUtil {

    /** 根日志: 在类里面使用 LoggerFactory.getLogger(XXX.class) 跟这种方式一样! */
    public static final Logger ROOT_LOG = LoggerFactory.getLogger("root");
    /** SQL 相关的日志 */
    public static final Logger SQL_LOG = LoggerFactory.getLogger("sqlLog");

    /** 接收到请求的时间戳  */
    private static final String START_REQUEST_TIME = "Start_Request_Time";
    /** 接收到请求的时间, 在配置文件中使用 %X{Record_Time} 获取  */
    private static final String RECEIVE_TIME = "Receive_Time";
    /** 在日志上下文中记录的跟踪 id */
    private static final String TRACE_ID = "Trace_Id";
    /** 在日志上下文中记录的请求信息: 包括 ip、url, param 等  */
    private static final String REQUEST_INFO = "Request_Info";
    /** 在日志上下文中记录的真实 ip */
    private static final String REAL_IP = "Real_Ip";
    /** 在日志上下文中记录的用户信息 */
    private static final String USER = "User";

    /** 将 跟踪号 和 接收到请求的时间 放进日志上下文 */
    public static void bindBasicInfo(String traceId) {
        if (U.isNull(MDC.get(START_REQUEST_TIME))) {
            Date now = new Date();
            MDC.put(START_REQUEST_TIME, U.toStr(now.getTime()));
            MDC.put(RECEIVE_TIME, DateUtil.formatDateTimeMs(now) + " -> ");
        }
        if (U.isNull(MDC.get(TRACE_ID))) {
            // xml 中没有加空格, 在值的前面加一个空格
            MDC.put(TRACE_ID, " " + (U.isNull(traceId) ? U.uuid16() : traceId));
        }
    }
    /** 日志上下文中没有 请求上下文信息 则返回 true */
    public static boolean hasNotRequestInfo() {
        return U.isNull(MDC.get(REQUEST_INFO));
    }
    /** 将 请求上下文信息 放进日志上下文 */
    public static void putContext(String traceId, RequestLogContext logContextInfo) {
        bindBasicInfo(traceId);
        MDC.put(REQUEST_INFO, logContextInfo.requestInfo());
    }
    public static void putIp(String ip) {
        MDC.put(REAL_IP, ip);
    }
    public static void putUser(String user) {
        if (U.isNotNull(user)) {
            MDC.put(USER, " " + user);
        }
    }

    public static void unbind() {
        MDC.clear();
    }

    public static long getStartMilli() {
        return U.toLong(MDC.get(START_REQUEST_TIME));
    }
    public static String getTraceId() {
        return U.toStr(MDC.get(TRACE_ID)).trim();
    }
    public static String getIp() {
        return MDC.get(REAL_IP);
    }
    public static String getUser() {
        return MDC.get(USER);
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class RequestLogContext {
        /** 访问方法 */
        private String method;
        /** 访问地址 */
        private String url;
        /** 请求 body 中的参数 */
        private String params;
        /** 请求 header 中的参数 */
        private String heads;

        RequestLogContext(String method, String url, String params, String heads) {
            this.method =method;
            this.url = url;
            this.params = params;
            this.heads = heads;
        }

        /** 输出 " [method url params(...) headers(...)]" */
        private String requestInfo() {
            StringBuilder sbd = new StringBuilder();
            sbd.append(" [");
            sbd.append(method).append(" ").append(url).append(")");
            if (U.isNotNull(heads)) {
                sbd.append(" headers(").append(heads).append(")");
            }
            if (U.isNotNull(params)) {
                sbd.append(" params(").append(params).append(")");
            }
            sbd.append("]");
            return sbd.toString();
        }
    }
}
