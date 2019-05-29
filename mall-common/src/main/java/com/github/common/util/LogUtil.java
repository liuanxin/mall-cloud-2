package com.github.common.util;

import com.github.common.date.DateUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** 日志管理, 使用此 utils 获取 log, 不要在类中使用 LoggerFactory.getLogger 的方式! */
public final class LogUtil {

    /** 根日志: 在类里面使用 LoggerFactory.getLogger(XXX.class) 跟这种方式一样! */
    public static final Logger ROOT_LOG = LoggerFactory.getLogger("root");

    /** SQL 相关的日志 */
    public static final Logger SQL_LOG = LoggerFactory.getLogger("sqlLog");


    /** 接收到请求的时间. 在 log.xml 中使用 %X{recordTime} 获取  */
    private static final String RECEIVE_TIME = "receiveTime";
    /** 请求信息: 包括 ip、url, param 等  */
    private static final String REQUEST_INFO = "requestInfo";

    /** 将当前请求的上下文信息放进日志 */
    public static void bind(RequestLogContext logContextInfo) {
        recordTime();
        MDC.put(REQUEST_INFO, logContextInfo.requestInfo());
    }
    public static void unbind() {
        MDC.clear();
    }
    public static boolean hasNotRequestInfo() {
        return U.isBlank(MDC.get(REQUEST_INFO));
    }

    public static void recordTime() {
        MDC.put(RECEIVE_TIME, DateUtil.nowTimeMs() + " -> ");
    }


    @Setter
    @Getter
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class RequestLogContext {
        private String id;
        private String name;
        /** 访问 ip */
        private String ip;
        /** 访问方法 */
        private String method;
        /** 访问地址 */
        private String url;
        /** 请求 body 中的参数 */
        private String params;
        /** 请求 header 中的参数 */
        private String heads;

        RequestLogContext(String ip, String method, String url, String params, String heads) {
            this.ip = ip;
            this.method =method;
            this.url = url;
            this.params = params;
            this.heads = heads;
        }

        /** 输出 " [ip (id/name) (method url) params(...) headers(...)]" */
        private String requestInfo() {
            // 参数 及 头 的长度如果超过 1100 就只输出前后 500 个字符
            int maxLen = 1100, headTail = 500;
            StringBuilder sbd = new StringBuilder();
            sbd.append(" [");
            sbd.append(ip);
            if (U.isNotBlank(id) || U.isNotBlank(name)) {
                sbd.append(" (");
                sbd.append(U.isBlank(id) ? U.EMPTY : id);
                sbd.append("/");
                sbd.append(U.isBlank(name) ? U.EMPTY : name);
                sbd.append(")");
            }
            sbd.append(" (").append(method).append(" ").append(url).append(")");

            sbd.append(" params(");
            int paramLen = params.length();
            if (paramLen > maxLen) {
                sbd.append(params, 0, headTail).append(" ... ").append(params, paramLen - headTail, paramLen);
            } else {
                sbd.append(params);
            }
            sbd.append(")");

            sbd.append(" headers(");
            int headLen = heads.length();
            if (headLen > maxLen) {
                sbd.append(heads, 0, headTail).append(" ... ").append(heads, headLen - headTail, headLen);
            } else {
                sbd.append(heads);
            }
            sbd.append(")");

            sbd.append("]");
            return sbd.toString();
        }
    }
}
