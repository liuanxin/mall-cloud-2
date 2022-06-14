package com.github.common.util;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * <span style="color:red;">
 * !!!
 * 此工具类请只在有 Request 上下文的地方调用(比如 Controller),
 * 在 Service 层调用意味着把 Request 的生命周期放到了更深的业务层.
 * 这不是一个好的习惯, 请不要这么做
 * !!!
 * </span> */
public final class RequestUtil {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String USER_AGENT = "User-Agent";
    private static final String REFERRER = "Referer";
    private static final String AJAX_KEY = "X-Requested-With";
    private static final String AJAX_VALUE = "XMLHttpRequest";
    private static final String NGINX_PROTO = "X-Forwarded-Proto";

    private static final String APPLICATION_JSON = "application/json";

    private static final String SCHEME = "//";
    private static final String HTTP = "http:" + SCHEME;
    private static final String HTTPS = "https:" + SCHEME;
    private static final String URL_SPLIT = "/";
    private static final String WWW = "www.";

    private static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE; // Locale.getDefault();

    /**
     * 获取真实客户端 ip, 关于 X-Forwarded-For 参考: http://zh.wikipedia.org/wiki/X-Forwarded-For<br>
     *
     * 这一 HTTP 头一般格式如: X-Forwarded-For: client1, proxy1, proxy2,<br><br>
     * 其中的值通过一个 逗号 + 空格 把多个 ip 地址区分开,
     * 最左边(client1)是最原始客户端的 ip 地址, 代理服务器每成功收到一个请求, 就把请求来源 ip 地址添加到右边
     */
    public static String getRealIp() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Cluster-Client-IP");
        if (U.isNotNull(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        return request.getRemoteAddr().split(",")[0].trim();
    }

    /** 获取请求协议, 通常是 http 和 https 两种. https 需要在 nginx 配置中添加 proxy_set_header X-Forwarded-Proto $scheme; 配置 */
    public static String getScheme() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String scheme = request.getHeader(NGINX_PROTO);
        if (U.isNull(scheme)) {
            scheme = request.getScheme();
        }
        return scheme.split(",")[0].trim().toLowerCase();
    }

    /** 获取请求的语言信息 */
    public static Locale getLocale() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? LocaleContextHolder.getLocale() : RequestContextUtils.getLocale(request);
    }

    /*** 本机就返回 true */
    public static boolean isLocalRequest() {
        return U.isLocalRequest(getRealIp());
    }

    /** 获取 ua 信息 */
    public static String userAgent() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? U.EMPTY : request.getHeader(USER_AGENT);
    }

    /** 如果是 ie 请求就返回 true */
    public static boolean isIeRequest() {
        String userAgent = userAgent();
        return U.isNotNull(userAgent) && userAgent.toUpperCase().contains("MSIE");
    }

    /** 判断当前请求是否来自移动端, 来自移动端则返回 true */
    public static boolean isMobileRequest() {
        return U.checkMobile(userAgent());
    }

    /** 判断当前请求是否是 ajax 请求, 是 ajax 则返回 true */
    public static boolean isAjaxRequest() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return false;
        }

        String requestedWith = request.getHeader(AJAX_KEY);
        if (U.isNotNull(requestedWith) && AJAX_VALUE.equalsIgnoreCase(requestedWith)) {
            return true;
        }

        String contentType = request.getHeader(CONTENT_TYPE);
        return (U.isNotNull(contentType) && APPLICATION_JSON.startsWith(contentType.toLowerCase()))
                || U.isNotNull(request.getParameter("_ajax"))
                || U.isNotNull(request.getParameter("_json"));
    }

    /** 请求头里的 referer 这个单词拼写是错误的, 应该是 referrer, 历史遗留问题 */
    public static String getReferrer() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? U.EMPTY : request.getHeader(REFERRER);
    }

    /** 获取请求地址, 比如请求的是 http://www.abc.com/x/y 将返回 /x/y */
    public static String getRequestUri() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? U.EMPTY : request.getRequestURI();
    }
    /** 获取请求地址, 如 http://www.abc.com/x/y */
    public static String getRequestUrl() {
        return getDomain() + getRequestUri();
    }

    /** 返回当前访问的域. 是 request.getRequestURL().toString() 中域的部分, 默认的 scheme 不会返回 https */
    public static String getDomain() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String scheme = getScheme();
        int port = request.getServerPort();
        boolean http = ("http".equals(scheme) && port != 80);
        boolean https = ("https".equals(scheme) && port != 80 && port != 443);

        StringBuilder sbd = new StringBuilder();
        sbd.append(scheme).append("://").append(request.getServerName());
        if (http || https) {
            sbd.append(":").append(port);
        }
        return sbd.toString();
    }

    /** 从 url 中获取 domain 信息. 如: http://www.jd.com/product/123 返回 http://www.jd.com */
    public static String getDomain(String url) {
        if (U.isNull(url)) {
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
        } else {
            return url.substring(0, url.indexOf(URL_SPLIT));
        }
    }

    /** 检查 url 在不在指定的域名中(以根域名检查, 如 www.qq.com 是以 qq.com 为准), 将所在根域名返回, 不在指定域名中则返回空 */
    public static String getDomainInUrl(String url, List<String> domainList) {
        url = getDomain(url);
        if (U.isNotNull(url) && A.isNotEmpty(domainList)) {
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
        return U.EMPTY;
    }

    /**
     * 格式化参数, 如果是文件流(form 表单中有 type="multipart/form-data" 这种), 则不打印出参数
     *
     * @return 示例: id=xxx&name=yyy
     */
    public static String formatParam() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String contentType = request.getContentType();
        boolean upload = U.isNotNull(contentType) && contentType.startsWith("multipart/");
        return upload ? "uploading file" : U.formatParam(request.getParameterMap());
    }

    /** 从 cookie 中获取值, 为空就从请求头中取, 为空再从参数中取 */
    public static String getCookieOrHeaderOrParam(String name) {
        String value = getCookieValue(name);
        return U.isEmpty(value) ? getHeaderOrParam(name) : value;
    }

    /** 先从请求头中查, 为空再从参数中查 */
    public static String getHeaderOrParam(String param) {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        String value = request.getHeader(param);
        if (U.isEmpty(value)) {
            value = request.getParameter(param);
        }
        return U.isEmpty(value) ? U.EMPTY : value.trim();
    }

    /** 从 cookie 中获取值 */
    public static String getCookieValue(String name) {
        Cookie cookie = getCookie(name);
        return U.isNull(cookie) ? U.EMPTY : cookie.getValue();
    }
    private static Cookie getCookie(String name) {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return null;
        }

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
        if (U.isNull(response)) {
            return;
        }

        Cookie cookie = getCookie(name);
        if (U.isNull(cookie)) {
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
    public static String formatHeader() {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return U.EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String headName = headers.nextElement();
            sbd.append("<");
            sbd.append(headName).append(" : ").append(DesensitizationUtil.desKey(headName, request.getHeader(headName)));
            sbd.append(">");
        }
        return sbd.toString();
    }


    /** 将「json 字符」以 json 格式输出 */
    public static void toJson(String data) {
        render(APPLICATION_JSON, data);
    }
    private static void render(String type, String data) {
        HttpServletResponse response = getResponse();
        if (U.isNull(response)) {
            return;
        }

        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("return data: " + data);
        }
        try {
            response.setCharacterEncoding("utf-8");
            response.setContentType(type + ";charset=utf-8;");
            response.getWriter().write(data);
        } catch (IllegalStateException e) {
            // 基于 response 调用了 getOutputStream(), 又再调用 getWriter() 会被 web 容器拒绝
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("response state exception", e);
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("handle json to {} io exception", type, e);
            }
        }
    }
    /** 将「json 字符」以 html 格式输出. 不常见! 这种只会在一些特殊的场景用到 */
    public static void toHtml(String data) {
        render("text/html", data);
    }

    /** 基于请求上下文生成一个日志需要的上下文信息对象 */
    public static LogUtil.RequestLogContext logContextInfo() {
        HttpServletRequest request = getRequest();
        String method = U.isNull(request) ? U.EMPTY : request.getMethod();
        String url = getRequestUrl();
        String param = formatParam();
        String head = formatHeader();
        return new LogUtil.RequestLogContext(method, url, param, head);
    }

    /**
     * 基于下面的优先级依次获取语言
     * 1. 头里的 langParamName(默认是 lang)
     * 2. 参数里的 langParamName(默认是 lang)
     * 3. request.getLocale() 头里的 Accept-Language
     * 4. 简体中文
     *
     * 手动处理时使用 {@link LocaleContextHolder#getLocale()} 或 {@link RequestContextUtils#getLocale(HttpServletRequest)}
     */
    public static void handleLocal(String langParamName) {
        HttpServletRequest request = getRequest();
        if (U.isNull(request)) {
            return;
        }

        if (U.isNull(langParamName)) {
            langParamName = "lang";
        }

        Locale locale = null;
        try {
            String lan = getHeaderOrParam(langParamName);
            if (U.isNotNull(lan)) {
                locale = StringUtils.parseLocale(lan);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("parse Local exception", e);
            }
        }

        if (U.isNull(locale) || U.isNull(locale.getCountry())) {
            locale = request.getLocale();
        }
        if (U.isNull(locale) || U.isNull(locale.getCountry())) {
            locale = DEFAULT_LOCALE;
        }
        LocaleContextHolder.setLocale(locale);
        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
        if (U.isNotNull(localeResolver)) {
            localeResolver.setLocale(request, getResponse(), locale);
        }
    }


    public static HttpServletRequest getRequest() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        return U.isNull(requestAttributes) ? null : requestAttributes.getRequest();
    }

    public static HttpSession getSession() {
        HttpServletRequest request = getRequest();
        return U.isNull(request) ? null : request.getSession();
    }

    public static HttpServletResponse getResponse() {
        ServletRequestAttributes requestAttributes = getRequestAttributes();
        return U.isNull(requestAttributes) ? null : requestAttributes.getResponse();
    }

    private static ServletRequestAttributes getRequestAttributes() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes());
    }
}
