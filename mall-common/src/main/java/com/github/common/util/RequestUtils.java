package com.github.common.util;

import com.github.common.json.JsonUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
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
    private static final String HTTPS = "https://";
    private static final String SCHEME = "//";
    private static final String URL_SPLIT = "/";
    private static final String WWW = "www.";

    /**
     * 获取真实客户端 ip, 关于 X-Forwarded-For 参考: http://zh.wikipedia.org/wiki/X-Forwarded-For<br>
     *
     * 这一 HTTP 头一般格式如: X-Forwarded-For: client1, proxy1, proxy2,<br><br>
     * 其中的值通过一个 逗号 + 空格 把多个 ip 地址区分开,
     * 最左边(client1)是最原始客户端的 ip 地址, 代理服务器每成功收到一个请求, 就把请求来源 ip 地址添加到右边
     */
    public static String getRealIp() {
        HttpServletRequest request = getRequest();

        String ip = request.getHeader("X-Forwarded-For");
        if (U.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
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

    /*** 本机就返回 true */
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

    /** 获取请求地址, 比如请求的是 http://www.abc.com/x/y 将返回 /x/y */
    public static String getRequestUri() {
        return getRequest().getRequestURI();
    }
    /** 获取请求地址, 如 http://www.abc.com/x/y */
    public static String getRequestUrl() {
        return getDomain() + getRequestUri();
    }

    /** 返回当前访问的域. 是 request.getRequestURL().toString() 中域的部分, 默认的 scheme 不会返回 https */
    public static String getDomain() {
        StringBuilder sbd = new StringBuilder();

        HttpServletRequest request = getRequest();
        String proxyScheme = request.getHeader("X-Forwarded-Proto");
        String scheme = U.isNotBlank(proxyScheme) ? proxyScheme : request.getScheme();

        sbd.append(scheme).append("://").append(request.getServerName());

        int port = request.getServerPort();
        boolean http = ("http".equalsIgnoreCase(scheme) && port != 80);
        boolean https = ("https".equalsIgnoreCase(scheme) && port != 80 && port != 443);
        if (http || https) {
            sbd.append(':').append(port);
        }
        return sbd.toString();
    }

    /** 从 url 中获取 domain 信息. 如: http://www.jd.com/product/123 返回 http://www.jd.com */
    public static String getDomain(String url) {
        if (U.isBlank(url)) {
            return U.EMPTY;
        }
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith(HTTP)) {
            String tmp = url.substring(HTTP.length());
            return url.substring(0, HTTP.length() + tmp.indexOf(URL_SPLIT));
        } else if (lowerUrl.startsWith(HTTPS)) {
            String tmp = url.substring(HTTPS.length());
            return url.substring(0, HTTPS.length() + tmp.indexOf(URL_SPLIT));
        } else if (lowerUrl.startsWith(SCHEME)) {
            String tmp = url.substring(SCHEME.length());
            return url.substring(0, SCHEME.length() + tmp.indexOf(URL_SPLIT));
        }
        return url.substring(0, url.indexOf(URL_SPLIT));
    }

    /** 检查 url 在不在指定的域名中(以根域名检查, 如 www.qq.com 是以 qq.com 为准), 将所在根域名返回, 不在指定域名中则返回空 */
    public static String getDomainInUrl(String url, List<String> domainList) {
        url = getDomain(url);
        if (U.isNotBlank(url) && A.isNotEmpty(domainList)) {
            for (String domain : domainList) {
                String lowerDomain = domain.toLowerCase();
                if (lowerDomain.startsWith(HTTP)) {
                    domain = domain.substring(HTTP.length());
                } else if (lowerDomain.startsWith(HTTPS)) {
                    domain = domain.substring(HTTPS.length());
                } else if (lowerDomain.startsWith(SCHEME)) {
                    domain = domain.substring(SCHEME.length());
                }

                if (domain.toLowerCase().startsWith(WWW)) {
                    domain = domain.substring(WWW.length());
                }
                if (url.toLowerCase().endsWith("." + domain.toLowerCase())) {
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

    /** 从 cookie 中获取值 */
    public static String getCookieValue(String name) {
        Cookie cookie = getCookie(name);
        return U.isBlank(cookie) ? U.EMPTY : cookie.getValue();
    }
    private static Cookie getCookie(String name) {
        HttpServletRequest request = getRequest();
        Cookie[] cookies = request.getCookies();
        if (A.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie;
                }
            }
        }
        return null;
    }
    /** 添加一个 http-only 的 cookie(浏览器环境中 js 用 document.cookie 获取时将会忽略) */
    public static void addHttpOnlyCookie(String name, String value, int second, int extendSecond) {
        HttpServletResponse response = getResponse();
        Cookie cookie = getCookie(name);
        if (U.isBlank(cookie)) {
            Cookie add = new Cookie(name, value);
            add.setPath("/");
            add.setHttpOnly(true);
            add.setMaxAge(second);
            response.addCookie(add);
        } else {
            int maxAge = cookie.getMaxAge();
            // 如果 cookie 中已经有值且过期时间在延长时间以内了, 则把 cookie 的过期时间延长到指定时间
            if (maxAge > 0 && maxAge < extendSecond && second > extendSecond) {
                cookie.setMaxAge(second);
                response.addCookie(cookie);
            }
        }
    }

    /** 格式化头里的参数: 键值以冒号分隔 */
    public static String formatHeadParam() {
        HttpServletRequest request = getRequest();

        StringBuilder sbd = new StringBuilder();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headName = headers.nextElement();
            sbd.append("<").append(headName).append(" : ").append(request.getHeader(headName)).append(">");
        }
        return sbd.toString();
    }


    /** 将「json 字符」以 json 格式输出 */
    public static <T> void toJson(T jsonResult) {
        render("application/json", jsonResult);
    }
    private static <T> void render(String type, T jsonResult) {
        String result = JsonUtil.toJson(jsonResult);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("return json: " + result);
        }
        try {
            HttpServletResponse response = getResponse();
            response.setCharacterEncoding("utf-8");
            response.setContentType(type + ";charset=utf-8;");
            response.getWriter().write(U.toStr(result));
        } catch (IllegalStateException e) {
            // 基于 response 调用了 getOutputStream(), 又再调用 getWriter() 会被 web 容器拒绝
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("response state exception", e);
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("handle json to %s io exception", type), e);
            }
        }
    }
    /** 将「json 字符」以 html 格式输出. 不常见! 这种只会在一些特殊的场景用到 */
    public static <T> void toHtml(T jsonResult) {
        render("text/html", jsonResult);
    }

    /** 基于请求上下文生成一个日志需要的上下文信息对象 */
    public static LogUtil.RequestLogContext logContextInfo() {
        HttpServletRequest request = getRequest();

        String ip = getRealIp();
        String method = request.getMethod();
        String url = getRequestUrl();
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
