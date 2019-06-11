package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.U;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 处理跨域 */
public class CorsFilter implements Filter {

    /** @see org.springframework.http.HttpHeaders */
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    // /** for ie: https://www.lovelucy.info/ie-accept-third-party-cookie.html */
    // private static final String P3P = "P3P";

    private void handlerCors(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(ORIGIN);
        String domain = getDomain(request);
        if (needResponseCors(origin, domain)) {
            if (U.isBlank(response.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN))) {
                response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            }
            if (U.isBlank(response.getHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS))) {
                response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }
            if (U.isBlank(response.getHeader(ACCESS_CONTROL_ALLOW_METHODS))) {
                response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, A.toStr(Const.SUPPORT_METHODS));
            }
            if (U.isBlank(response.getHeader(ACCESS_CONTROL_ALLOW_HEADERS))) {
                // 如果有自定义头, 附加进去, 避免用 *
                response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS,
                                "Accept, Accept-Encoding, Accept-Language, Cache-Control, " +
                                "Connection, Cookie, DNT, Host, User-Agent, Content-Type, Authorization, " +
                                "X-Requested-With, Origin, Access-Control-Request-headers, " +
                                Const.TOKEN + ", " + Const.VERSION);
            }
            /*
            if (request.getHeader("user-agent").toUpperCase().contains("MSIE") && U.isBlank(response.getHeader(P3P))) {
                response.addHeader(P3P, "CP='CAO IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT'");
            }
            */
        }
    }
    /** 获取请求域 */
    private String getDomain(HttpServletRequest request) {
        StringBuilder domain = new StringBuilder();

        String scheme = request.getScheme();
        int port = request.getServerPort();

        domain.append(scheme).append("://").append(request.getServerName());
        if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
            domain.append(':');
            domain.append(port);
        }
        return domain.toString();
    }
    /** 需要响应 cors 头则返回 true */
    private boolean needResponseCors(String origin, String domain) {
        // 如果 origin 和 domain 是一样的, 响应 cors 没什么必要
        if (U.isBlank(origin) || U.isBlank(domain) || origin.equals(domain)) {
            return false;
        }

        // 如果当前页是 http://manager.google.com, 请求的是 http://api.google.com, 也就是根域是一样的则表示需要响应 cors
        String rootOrigin = getRootDomain(origin);
        String rootDomain = getRootDomain(domain);
        if (rootOrigin.equals(rootDomain)) {
            return true;
        }
        // 如果当前页是 http://www.taobao.com, 请求的是 http://www.tmall.com, 也就是同公司的不同域名, 也需要响应 cors
        // ...

        return true;
    }
    private String getRootDomain(String domain) {
        if (!domain.contains(".")) {
            return domain;
        }
        if (domain.contains("//")) {
            domain = domain.substring(domain.indexOf("//") + 2);
        }
        String root = domain.substring(domain.indexOf(".") + 1);
        return root.contains(".") ? root : domain;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        handlerCors((HttpServletRequest) request, (HttpServletResponse) response);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
