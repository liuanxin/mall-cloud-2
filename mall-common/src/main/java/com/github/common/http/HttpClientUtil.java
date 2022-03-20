package com.github.common.http;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.common.Const;
import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpClientUtil {

    private static final String USER_AGENT = "Mozilla/5.0 (httpclient4.5; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36";

    /** 重试次数 */
    private static final int RETRY_COUNT = 3;
    /** 每个连接的最大连接数, 默认是 20 */
    private static final int MAX_CONNECTIONS = 200;
    /** 每个连接的路由数, 默认是 2 */
    private static final int MAX_CONNECTIONS_PER_ROUTE = 50;

    /** 从连接池获取到连接的超时时间, 单位: 毫秒 */
    private static final int CONNECTION_REQUEST_TIME_OUT = 3000;
    /** 建立连接的超时时间, 单位: 毫秒 */
    private static final int CONNECT_TIME_OUT = 5000;
    /** 数据交互的时间, 单位: 毫秒 */
    private static final int SOCKET_TIME_OUT = 60000;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER;
    static {
        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager();

        // 设置每个连接的路由数, 默认是 2
        CONNECTION_MANAGER.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        // 每个连接的最大连接数, 默认是 20
        CONNECTION_MANAGER.setMaxTotal(MAX_CONNECTIONS);

        // 重试策略
        HTTP_REQUEST_RETRY_HANDLER = (exception, executionCount, context) -> {
            if (executionCount > RETRY_COUNT) {
                return false;
            }

            Class<? extends IOException> methodThrowClass = exception.getClass();
            List<Class<? extends IOException>> retryClasses = List.of(
                    NoHttpResponseException.class // 服务器未响应时
            );
            for (Class<? extends IOException> clazz : retryClasses) {
                // parent.isAssignableFrom(child) ==> true, child.isAssignableFrom(parent) ==> false
                if (clazz == methodThrowClass || methodThrowClass.isAssignableFrom(clazz)) {
                    return true;
                }
            }

            List<Class<? extends IOException>> noRetryClasses = List.of(
                    SSLException.class, // SSL 异常
                    InterruptedIOException.class, // 超时
                    UnknownHostException.class, // 目标服务器不可达
                    ConnectException.class // 连接异常
            );
            for (Class<? extends IOException> clazz : noRetryClasses) {
                // parent.isAssignableFrom(child) ==> true, child.isAssignableFrom(parent) ==> false
                if (clazz == methodThrowClass || methodThrowClass.isAssignableFrom(clazz)) {
                    return false;
                }
            }

            HttpRequest request = HttpClientContext.adapt(context).getRequest();
            // 如果请求是幂等的就重试
            return !(request instanceof HttpEntityEnclosingRequest);
        };
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setUserAgent(USER_AGENT)
                .setConnectionManager(CONNECTION_MANAGER)
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER)
                .build();
    }
    private static RequestConfig config(int connectTimeout, int socketTimeout) {
        // 配置请求的超时设置
        return RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIME_OUT)
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .build();
    }


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        return handleRequest(new HttpGet(url), null, CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    public static String get(String url, int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        return handleRequest(new HttpGet(url), null, connectTimeout, socketTimeout);
    }
    public static <T> String get(String url, T param) {
        if (U.isNull(url)) {
            return null;
        }

        Map<String, Object> params = Collections.emptyMap();
        if (A.isNotEmpty(param)) {
            params = JsonUtil.convertType(param, new TypeReference<>() {});
        }
        return get(url, params);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);
        return handleRequest(new HttpGet(url), U.formatParam(params), CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params, int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);
        return handleRequest(new HttpGet(url), U.formatParam(params), connectTimeout, socketTimeout);
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);

        HttpGet request = new HttpGet(url);
        handleHeader(request, headerMap);
        return handleRequest(request, U.formatParam(params), CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap,
                                       int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);

        HttpGet request = new HttpGet(url);
        handleHeader(request, headerMap);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }


    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        return handleRequest(request, U.formatParam(params), CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params, int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }
    /** 向指定的 url 进行 post 请求. 参数以 json 的方式一次传递 */
    public static String post(String url, String json) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求. 参数以 json 的方式一次传递 */
    public static String post(String url, String json, int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        handleHeader(request, headers);
        return handleRequest(request, U.formatParam(params), CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers,
                                        int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        handleHeader(request, headers);
        return handleRequest(request, U.formatParam(params), connectTimeout, socketTimeout);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postBodyWithHeader(String url, String json, Map<String, Object> headers) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postBodyWithHeader(String url, String json, Map<String, Object> headers,
                                            int connectTimeout, int socketTimeout) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        handleHeader(request, headers);
        request.addHeader("Content-Type", "application/json");
        return handleRequest(request, json, connectTimeout, socketTimeout);
    }


    /** 向指定的 url 进行 post 操作, 有参数和文件 */
    public static String postFile(String url, Map<String, Object> params, Map<String, File> files) {
        if (U.isNull(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        if (A.isEmpty(params)) {
            params = new HashMap<>();
        }
        HttpPost request = handlePostParams(url, params);
        if (A.isNotEmpty(files)) {
            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create().setLaxMode();
            for (Map.Entry<String, File> entry : files.entrySet()) {
                String key = entry.getKey();
                File value = entry.getValue();

                entityBuilder.addBinaryBody(key, value);
                params.put(key, value.toString());
            }
            request.setEntity(entityBuilder.build());
        }
        return handleRequest(request, U.formatParam(params), CONNECT_TIME_OUT, SOCKET_TIME_OUT);
    }


    /** url 如果不是以 http:// 或 https:// 开头就加上 http:// */
    private static String handleEmptyScheme(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        return url;
    }
    /** 处理 get 请求的参数: 拼在 url 上即可 */
    private static String handleGetParams(String url, Map<String, Object> params) {
        if (A.isNotEmpty(params)) {
            url = U.appendUrl(url) + U.formatParam(params);
        }
        return url;
    }
    /** 处理 post 请求的参数 */
    private static HttpPost handlePostParams(String url, Map<String, Object> params) {
        HttpPost request = new HttpPost(url);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        if (A.isNotEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (U.isNotNull(key) && U.isNotNull(value)) {
                    nameValuePairs.add(new BasicNameValuePair(key, A.toString(value)));
                }
            }
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs, StandardCharsets.UTF_8));
        }
        return request;
    }
    /** 处理请求时存到 header 中的数据 */
    private static void handleHeader(HttpRequestBase request, Map<String, Object> headers) {
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                Object value = entry.getValue();
                if (U.isNotNull(value)) {
                    request.addHeader(entry.getKey(), value.toString());
                }
            }
        }
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params,
                                         Header[] requestHeaders, Header[] responseHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        sbd.append("HttpClient4 => [")
                .append(DateUtil.formatDateTimeMs(new Date(start))).append(" -> ").append(DateUtil.nowDateTimeMs())
                .append("] (").append(method).append(" ").append(url).append(")");

        if (U.isNotNull(params)) {
            sbd.append(" param(").append(U.compress(params)).append(") ");
        }
        if (A.isNotEmpty(requestHeaders)) {
            sbd.append(" request headers(");
            for (Header header : requestHeaders) {
                sbd.append("<").append(header.getName()).append(": ").append(header.getValue()).append(">");
            }
            sbd.append(")");
        }

        sbd.append(",");

        if (A.isNotEmpty(responseHeaders)) {
            sbd.append(" response headers(");
            for (Header header : responseHeaders) {
                sbd.append("<").append(header.getName()).append(": ").append(header.getValue()).append(">");
            }
            sbd.append(")");
        }
        sbd.append(" return(").append(U.compress(result)).append(")");
        return sbd.toString();
    }
    /** 发起 http 请求 */
    private static String handleRequest(HttpRequestBase request, String params, int connectTimeout, int socketTimeout) {
        request.setConfig(config(connectTimeout, socketTimeout));
        request.addHeader("Content-Type", "application/json");

        String traceId = LogUtil.getTraceId();
        if (U.isNotNull(traceId)) {
            request.addHeader(Const.TRACE, traceId);
        }
        String method = request.getMethod();
        String url = request.getURI().toString();

        long start = System.currentTimeMillis();
        try (CloseableHttpResponse response = createHttpClient().execute(request, HttpClientContext.create())) {
            HttpEntity entity = response.getEntity();
            if (U.isNotNull(entity)) {
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    Header[] reqHeaders = request.getAllHeaders();
                    Header[] resHeaders = response.getAllHeaders();
                    LogUtil.ROOT_LOG.info(collectContext(start, method, url, params, reqHeaders, resHeaders, result));
                }
                EntityUtils.consume(entity);
                return result;
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("{} => {} exception", method, url, e);
            }
        }
        return null;
    }


    /** 用 get 方式请求 url 并将响应结果保存指定的文件 */
    public static void download(String url, String file) {
        url = handleEmptyScheme(url);
        HttpGet request = new HttpGet(url);
        request.setConfig(config(CONNECT_TIME_OUT, SOCKET_TIME_OUT));

        long start = System.currentTimeMillis();
        try (CloseableHttpResponse response = createHttpClient().execute(request, HttpClientContext.create())) {
            HttpEntity entity = response.getEntity();
            if (U.isNotNull(entity)) {
                entity.writeTo(new FileOutputStream(file));
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    long ms = (System.currentTimeMillis() - start);
                    LogUtil.ROOT_LOG.info("download ({}) to file({}) success, time({}ms)", url, file, ms);
                }
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("download ({}) to file({}) exception", url, file, e);
            }
        }
    }
}
