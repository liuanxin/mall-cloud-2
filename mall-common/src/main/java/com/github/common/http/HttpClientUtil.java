package com.github.common.http;

import com.github.common.date.DateUtil;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HttpClientUtil {

    private static final int TIME_OUT = 30 * 1000;

    private static final PoolingHttpClientConnectionManager CONNECTION_MANAGER;
    private static final HttpRequestRetryHandler HTTP_REQUEST_RETRY_HANDLER;
    private static final int RETRY_COUNT = 3;
    static {
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        SSLContext ignoreVerifySSL = TrustAllCerts.SSL_CONTEXT;
        if (U.isBlank(ignoreVerifySSL)) {
            sslConnectionSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
        } else {
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(ignoreVerifySSL);
        }

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", sslConnectionSocketFactory)
                .build();

        CONNECTION_MANAGER = new PoolingHttpClientConnectionManager(registry);
        // 连接池中的最大连接数默认是 20
        // CONNECTION_MANAGER.setMaxTotal(20);

        // 重试策略
        HTTP_REQUEST_RETRY_HANDLER = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                if (executionCount > RETRY_COUNT) {
                    return false;
                }
                // 服务器未响应时重试
                if (exception instanceof NoHttpResponseException) {
                    return true;
                }
                // SSL 握手异常时不重试
                if (exception instanceof SSLHandshakeException || exception instanceof SSLException) {
                    return false;
                }
                // 超时时不重试
                if (exception instanceof InterruptedIOException) {
                    return false;
                }
                // 目标服务器不可达时不重试
                if (exception instanceof UnknownHostException) {
                    return false;
                }

                HttpRequest request = HttpClientContext.adapt(context).getRequest();
                // 如果请求是幂等的就重试
                return !(request instanceof HttpEntityEnclosingRequest);
            }
        };
    }

    private static CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
                .setConnectionManager(CONNECTION_MANAGER)
                .setRetryHandler(HTTP_REQUEST_RETRY_HANDLER).build();
    }
    private static void config(HttpRequestBase request) {
        // 配置请求的超时设置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(TIME_OUT)
                .setConnectTimeout(TIME_OUT)
                .setSocketTimeout(TIME_OUT).build();
        request.setConfig(requestConfig);
    }


    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        return handleRequest(new HttpGet(url), null);
    }
    @SuppressWarnings("unchecked")
    public static <T> String get(String url, T param) {
        if (U.isBlank(url)) {
            return null;
        }

        Map<String, Object> params = Collections.emptyMap();
        if (U.isNotBlank(param)) {
            params = JsonUtil.convert(param, Map.class);
        }
        return get(url, params);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);
        return handleRequest(new HttpGet(url), U.formatParam(params));
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        url = handleGetParams(url, params);

        HttpGet request = new HttpGet(url);
        handleHeader(request, headerMap);
        return handleRequest(request, U.formatParam(params));
    }


    @SuppressWarnings("unchecked")
    public static <T> String post(String url, T param) {
        if (U.isBlank(url)) {
            return null;
        }

        Map<String, Object> params = Collections.emptyMap();
        if (U.isNotBlank(param)) {
            params = JsonUtil.convert(param, Map.class);
        }
        return post(url, params);
    }
    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        return handleRequest(request, U.formatParam(params));
    }
    /** 向指定的 url 进行 post 请求. 参数以 json 的方式一次传递 */
    public static String post(String url, String json) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = new HttpPost(url);
        request.setEntity(new ByteArrayEntity(json.getBytes(StandardCharsets.UTF_8)));
        return handleRequest(request, json);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        HttpPost request = handlePostParams(url, params);
        handleHeader(request, headers);
        return handleRequest(request, U.formatParam(params));
    }


    /** 向指定的 url 进行 post 操作, 有参数和文件 */
    public static String postFile(String url, Map<String, Object> params, Map<String, File> files) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleEmptyScheme(url);
        if (A.isEmpty(params)) {
            params = Maps.newHashMap();
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
        return handleRequest(request, U.formatParam(params));
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
        List<NameValuePair> nameValuePairs = Lists.newArrayList();
        if (A.isNotEmpty(params)) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (U.isNotBlank(key) && U.isNotBlank(value)) {
                    nameValuePairs.add(new BasicNameValuePair(key, A.toStringWithArrayOrCollection(value)));
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
                if (U.isNotBlank(value)) {
                    request.addHeader(entry.getKey(), value.toString());
                }
            }
        }
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params,
                                         Header[] requestHeaders, Header[] responseHeaders, String result) {
        StringBuilder sbd = new StringBuilder();
        sbd.append("HttpClient => [")
                .append(DateUtil.formatMs(new Date(start))).append(" -> ").append(DateUtil.nowTimeMs())
                .append("] (").append(method).append(" ").append(url).append(")");
        // 参数 及 头 的长度如果超过 1100 就只输出前后 500 个字符
        int maxLen = 1100, headTail = 500;

        if (U.isNotBlank(params)) {
            sbd.append(" param(");
            int len = params.length();
            if (len > maxLen) {
                sbd.append(params, 0, headTail).append(" <.> ").append(params, len - headTail, len);
            } else {
                sbd.append(params);
            }
            sbd.append(") ");
        }
        if (A.isNotEmpty(requestHeaders)) {
            sbd.append(" request headers(");
            for (Header header : requestHeaders) {
                sbd.append("<").append(header.getName()).append(" : ").append(header.getValue()).append(">");
            }
            sbd.append(")");
        }

        sbd.append(",");

        if (A.isNotEmpty(responseHeaders)) {
            sbd.append(" response headers(");
            for (Header header : responseHeaders) {
                sbd.append("<").append(header.getName()).append(" : ").append(header.getValue()).append(">");
            }
            sbd.append(")");
        }
        sbd.append(" return(");
        if (U.isNotBlank(result)) {
            int len = result.length();
            if (len > maxLen) {
                sbd.append(result, 0, headTail).append(" ... ").append(result, len - headTail, len);
            } else {
                sbd.append(result);
            }
        }
        sbd.append(")");
        return sbd.toString();
    }
    /** 发起 http 请求 */
    private static String handleRequest(HttpRequestBase request, String params) {
        String method = request.getMethod();
        String url = request.getURI().toString();

        config(request);
        long start = System.currentTimeMillis();
        try (CloseableHttpResponse response = createHttpClient().execute(request, HttpClientContext.create())) {
            HttpEntity entity = response.getEntity();
            if (U.isNotBlank(entity)) {
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    Header[] requestHeaders = request.getAllHeaders();
                    Header[] responseHeaders = response.getAllHeaders();
                    String log = collectContext(start, method, url, params, requestHeaders, responseHeaders, result);
                    LogUtil.ROOT_LOG.info(log);
                }
                EntityUtils.consume(entity);
                return result;
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("(%s %s) exception", method, url), e);
            }
        }
        return null;
    }


    /** 用 get 方式请求 url 并将响应结果保存指定的文件 */
    public static void download(String url, String file) {
        url = handleEmptyScheme(url);
        HttpGet request = new HttpGet(url);

        config(request);
        long start = System.currentTimeMillis();
        try (CloseableHttpResponse response = createHttpClient().execute(request, HttpClientContext.create())) {
            HttpEntity entity = response.getEntity();
            if (U.isNotBlank(entity)) {
                entity.writeTo(new FileOutputStream(new File(file)));
                if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                    long ms = (System.currentTimeMillis() - start);
                    LogUtil.ROOT_LOG.info("download ({}) to file({}) success, time({}ms)", url, file, ms);
                }
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("download (%s) to file(%s) exception", url, file), e);
            }
        }
    }
}
