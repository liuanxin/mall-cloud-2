package com.github.common.util;

import com.github.common.json.JsonResult;
import com.github.common.json.JsonUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

/** <span style="color:red;">!!!此工具类请只在 Controller 中调用!!!</span> */
public final class RequestUtils {

    private static final String USER_AGENT = "user-agent";
    private static final String REFERRER = "referer";

    private static final String HTTP = "http://";
    private static final String HTTPS = "http://";
    private static final String SCHEME = "//";
    private static final String URL_SPLIT = "/";
    private static final String WWW = "www.";

    /**
     * 获取真实客户端IP
     * 关于 X-Forwarded-For 参考: http://zh.wikipedia.org/wiki/X-Forwarded-For<br>
     * 这一 HTTP 头一般格式如下:
     * X-Forwarded-For: client1, proxy1, proxy2,<br><br>
     * 其中的值通过一个 逗号 + 空格 把多个 IP 地址区分开, 最左边(client1)是最原始客户端的IP地址,
     * 代理服务器每成功收到一个请求，就把请求来源IP地址添加到右边
     */
    public static String getRealIp() {
        HttpServletRequest request = getRequest();

        String ip = request.getHeader("X-Forwarded-For");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为 真实 ip
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        ip = request.getHeader("X-Cluster-Client-IP");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /*** 是否是本机 */
    public static boolean isLocalRequest() {
        return U.isLocalRequest(getRealIp());
    }

    public static String userAgent() {
        return getRequest().getHeader(USER_AGENT);
    }

    /** 如果是 ie 请求就返回 true */
    public static boolean isIeRequest() {
        return userAgent().toUpperCase().contains("MSIE");
    }

    /** 判断当前请求是否来自移动端, 来自移动端则返回 true */
    public static boolean isMobileRequest() {
        return U.checkMobile(userAgent());
    }

    /** 判断当前请求是否是 ajax 请求, 是 ajax 则返回 true */
    public static boolean isAjaxRequest() {
        HttpServletRequest request = getRequest();

        String requestedWith = request.getHeader("X-Requested-With");
        if (U.isNotBlank(requestedWith) && "XMLHttpRequest".equals(requestedWith)) {
            return true;
        }

        String contentType = request.getHeader("Content-Type");
        return (U.isNotBlank(contentType) && "application/json".startsWith(contentType))
                || U.isNotBlank(request.getParameter("_ajax"))
                || U.isNotBlank(request.getParameter("_json"));
    }

    /** 请求头里的 referer 这个单词拼写是错误的, 应该是 referrer, 历史遗留问题 */
    public static String getReferrer() {
        return getRequest().getHeader(REFERRER);
    }

    /** 返回当前访问的域. 是 request.getRequestURL().toString() 中域的部分 */
    public static String getDomain() {
        StringBuilder domain = new StringBuilder();

        HttpServletRequest request = getRequest();
        String scheme = request.getScheme();
        int port = request.getServerPort();
        boolean http = ("http".equals(scheme) && port != 80);
        boolean https = ("https".equals(scheme) && port != 443);

        domain.append(scheme).append("://").append(request.getServerName());
        if (http || https) {
            domain.append(':');
            domain.append(port);
        }
        return domain.toString();
    }

    /** 从 url 中获取 domain 信息. 如: http://www.jd.com/product/123 返回 http://www.jd.com */
    public static String getDomain(String url) {
        if (U.isBlank(url)) {
            return U.EMPTY;
        }
        if (url.startsWith(HTTP)) {
            String tmp = url.substring(HTTP.length());
            return url.substring(0, HTTP.length() + tmp.indexOf(URL_SPLIT));
        } else if (url.startsWith(HTTPS)) {
            String tmp = url.substring(HTTPS.length());
            return url.substring(0, HTTPS.length() + tmp.indexOf(URL_SPLIT));
        } else if (url.startsWith(SCHEME)) {
            String tmp = url.substring(SCHEME.length());
            return url.substring(0, SCHEME.length() + tmp.indexOf(URL_SPLIT));
        }
        return url.substring(0, url.indexOf(URL_SPLIT));
    }

    /** 检查 url 在不在指定的域名中(以根域名检查, 如 www.qq.com 是以 qq.com 为准), 将所在根域名返回, 不在指定域名中则返回空 */
    public static String getDomainInUrl(String url, List<String> domainList) {
        url = getDomain(url);
        if (U.isNotBlank(url)) {
            for (String domain : domainList) {
                if (domain.startsWith(HTTP)) {
                    domain = domain.substring(HTTP.length());
                } else if (domain.startsWith(HTTPS)) {
                    domain = domain.substring(HTTPS.length());
                } else if (domain.startsWith(SCHEME)) {
                    domain = domain.substring(SCHEME.length());
                }
                if (domain.startsWith(WWW)) {
                    domain = domain.substring(WWW.length());
                }
                if (url.endsWith("." + domain)) {
                    return domain;
                }
            }
        }
        return null;
    }

    /**
     * 格式化参数, 如果是文件流(form 表单中有 type="multipart/form-data" 这种), 则不打印出参数
     *
     * @return 示例: id=xxx&name=yyy
     */
    public static String formatParam() {
        // return getRequest().getQueryString(); // 没有时将会返回 null

        HttpServletRequest request = getRequest();
        String contentType = request.getContentType();
        boolean upload = U.isNotBlank(contentType) && contentType.startsWith("multipart/");
        return upload ? "uploading file" : U.formatParam(request.getParameterMap());
    }

    /** 先从请求头中查, 为空再从参数中查 */
    public static String getHeaderOrParam(String param) {
        HttpServletRequest request = getRequest();
        String value = request.getHeader(param);
        if (U.isBlank(value)) {
            value = request.getParameter(param);
        }
        return U.isBlank(value) ? U.EMPTY : value.trim();
    }

    /** 格式化头里的参数: 键值以冒号分隔 */
    public static String formatHeadParam() {
        HttpServletRequest request = getRequest();

        StringBuilder sbd = new StringBuilder();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            sbd.append("<");
            String headName = headerNames.nextElement();
            sbd.append(headName).append(" : ").append(request.getHeader(headName));
            sbd.append(">");
        }
        return sbd.toString();
    }


    /** 将「json 字符」以 json 格式输出 */
    public static void toJson(JsonResult result, HttpServletResponse response) throws IOException {
        render("application/json", result, response);
    }
    private static void render(String type, JsonResult jsonResult, HttpServletResponse response) throws IOException {
        String result = JsonUtil.toJson(jsonResult);
        if (LogUtil.ROOT_LOG.isInfoEnabled()) {
            LogUtil.ROOT_LOG.info("return json: " + result);
        }

        try {
            response.setCharacterEncoding("utf-8");
            response.setContentType(type + ";charset=utf-8;");
            response.getWriter().write(result);
        } catch (IllegalStateException e) {
            // 基于 response 调用了 getOutputStream(), 又再调用 getWriter() 会被 web 容器拒绝
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("response state exception", e);
            }
        }
    }
    /** 将「json 字符」以 html 格式输出. 不常见! 这种只会在一些特殊的场景用到 */
    public static void toHtml(JsonResult result, HttpServletResponse response) throws IOException {
        render("text/html", result, response);
    }

    /** 基于请求上下文生成一个日志需要的上下文信息对象 */
    public static LogUtil.RequestLogContext logContextInfo() {
        HttpServletRequest request = getRequest();

        String ip = getRealIp();
        String method = request.getMethod();
        String url = request.getRequestURL().toString();
        String param = formatParam();
        String headParam = formatHeadParam();
        return new LogUtil.RequestLogContext(ip, method, url, param, headParam);
    }


    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    private static ServletRequestAttributes getRequestAttributes() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
    }
}
