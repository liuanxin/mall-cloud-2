package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.U;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/** 处理跨域 */
public class CorsFilter implements Filter {

    private static final String ORIGIN = "Origin";
    private static final String CREDENTIALS = "true";
    private static final String METHODS = A.toStr(Const.SUPPORT_METHODS);
    private static final String HEADERS = A.toStr(Const.ALLOW_HEADERS);

    /** @see org.springframework.http.HttpHeaders */
    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    // /** for ie: https://www.lovelucy.info/ie-accept-third-party-cookie.html */
    // private static final String P3P = "P3P";
    // private static final String IEP3P = "CP='CAO IDC DSP COR ADM DEVi TAIi PSA PSD IVAi IVDi CONi HIS OUR IND CNT'";

    private final Set<String> allowOriginSet;
    public CorsFilter(String allowHeaders) {
        Set<String> allowOriginSet = new HashSet<>();
        if (U.isNotNull(allowHeaders)) {
            // 英文逗号 或 空格
            for (String allowHeader : allowHeaders.split("[, ]")) {
                allowOriginSet.add(allowHeader.trim());
            }
        }
        this.allowOriginSet = allowOriginSet;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader(ORIGIN);
        if (U.isNotNull(origin)) {
            String domain = getDomain(request);
            // 头里面带过来的 origin 跟请求的不一样才需要设置 cors
            if (U.isNotEquals(domain, origin)) {
                // 配置项为空则设置 cors
                if (A.isEmpty(allowOriginSet)) {
                    setOrigin(response, origin);
                } else {
                    // 头里面带过来的 origin 是在配置项里面才设置 cors
                    if (allowOriginSet.contains(origin)) {
                        setOrigin(response, origin);
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
    private void setOrigin(HttpServletResponse response, String origin) {
        response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, CREDENTIALS);
        response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, METHODS);
        response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, HEADERS);
        /*
        if (RequestUtils.isIeRequest() && U.isEmpty(response.getHeader(P3P))) {
            response.addHeader(P3P, IEP3P);
        }
        */
    }
    private String getDomain(HttpServletRequest request) {
        StringBuilder sbd = new StringBuilder();
        String scheme = request.getScheme();
        int port = request.getServerPort();
        sbd.append(scheme).append("://").append(request.getServerName());
        boolean http = ("http".equals(scheme) && port != 80);
        boolean https = ("https".equals(scheme) && port != 80 && port != 443);
        if (http || https) {
            sbd.append(':').append(port);
        }
        return sbd.toString();
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
