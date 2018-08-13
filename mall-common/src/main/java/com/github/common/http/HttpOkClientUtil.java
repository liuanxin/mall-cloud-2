package com.github.common.http;

import com.google.common.io.Files;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import okhttp3.*;

import javax.activation.MimetypesFileTypeMap;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpOkClientUtil {

    // MIME 说明: http://www.w3school.com.cn/media/media_mimeref.asp

    private static final int TIME_OUT = 30;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient HTTP_CLIENT;
    static {
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                // 连接超时时间
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                // 响应超时时间
                .readTimeout(TIME_OUT, TimeUnit.SECONDS)
                // 连接池中的最大连接数默认是 5 且每个连接保持 5 分钟
                // .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES));
                .connectionPool(new ConnectionPool());

        SSLContext ignoreVerifySSL = TrustAllCerts.SSL_CONTEXT;
        if (U.isNotBlank(ignoreVerifySSL)) {
            builder.sslSocketFactory(ignoreVerifySSL.getSocketFactory(), TrustAllCerts.INSTANCE);
        }
        HTTP_CLIENT = builder.build();
    }

    /** 向指定 url 进行 get 请求 */
    public static String get(String url) {
        if (U.isBlank(url)) {
            return null;
        }

        return handleRequest(url, new Request.Builder(), null);
    }
    /** 向指定 url 进行 get 请求. 有参数 */
    public static String get(String url, Map<String, Object> params) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleGetParams(url, params);
        return handleRequest(url, new Request.Builder(), U.formatParam(params));
    }
    /** 向指定 url 进行 get 请求. 有参数和头 */
    public static String getWithHeader(String url, Map<String, Object> params, Map<String, Object> headerMap) {
        if (U.isBlank(url)) {
            return null;
        }

        url = handleGetParams(url, params);
        Request.Builder builder = new Request.Builder();
        handleHeader(builder, headerMap);
        return handleRequest(url, builder, U.formatParam(params));
    }


    /** 向指定的 url 进行 post 请求. 有参数 */
    public static String post(String url, Map<String, Object> params) {
        if (U.isBlank(url)) {
            return null;
        }

        Request.Builder builder = handlePostParams(params);
        return handleRequest(url, builder, U.formatParam(params));
    }
    /** 向指定的 url 进行 post 请求. 参数以 json 的方式一次传递 */
    public static String post(String url, String json) {
        if (U.isBlank(url)) {
            return null;
        }

        RequestBody body = RequestBody.create(JSON, json);
        Request.Builder builder = new Request.Builder().post(body);
        return handleRequest(url, builder, json);
    }
    /** 向指定的 url 进行 post 请求. 有参数和头 */
    public static String postWithHeader(String url, Map<String, Object> params, Map<String, Object> headers) {
        if (U.isBlank(url)) {
            return null;
        }

        Request.Builder builder = handlePostParams(params);
        handleHeader(builder, headers);
        return handleRequest(url, builder, U.formatParam(params));
    }


    /** 向指定 url 上传 png 图片文件 */
    public static String postFile(String url, Map<String, Object> params, Map<String, File> files) {
        if (U.isBlank(url)) {
            return null;
        }

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (U.isNotBlank(value)) {
                builder.addFormDataPart(entry.getKey(), value.toString());
            }
        }
        for (Map.Entry<String, File> entry : files.entrySet()) {
            File file = entry.getValue();
            if (U.isNotBlank(file)) {
                MediaType type = MediaType.parse(new MimetypesFileTypeMap().getContentType(file));

                RequestBody body = RequestBody.create(type, file);
                builder.addFormDataPart(entry.getKey(), null, body);
            }
        }
        Request.Builder request = new Request.Builder().post(builder.build());
        return handleRequest(url, request, U.formatParam(params));
    }


    /** url 如果不是以 「http://」 或 「https://」 开头就加上 「http://」 */
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
    private static Request.Builder handlePostParams(Map<String, Object> params) {
        return new Request.Builder().post(RequestBody.create(MultipartBody.FORM, U.formatParam(params)));
    }
    /** 处理请求时存到 header 中的数据 */
    private static void handleHeader(Request.Builder request, Map<String, Object> headers) {
        if (A.isNotEmpty(headers)) {
            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (U.isNotBlank(value)) {
                    request.addHeader(key, value.toString());
                }
            }
        }
    }
    /** 收集上下文中的数据, 以便记录日志 */
    private static String collectContext(long start, String method, String url, String params,
                                         Headers requestHeaders, Headers responseHeaders, String result) {
        long ms = System.currentTimeMillis() - start;
        StringBuilder sbd = new StringBuilder();
        sbd.append("OkHttp3 => (").append(method).append(" ").append(url).append(")");
        if (U.isNotBlank(params)) {
            sbd.append(" params(").append(params).append(")");
        }
        if (U.isNotBlank(requestHeaders)) {
            sbd.append(" request headers(");
            for (String name : requestHeaders.names()) {
                sbd.append("<").append(name).append(" : ").append(requestHeaders.get(name)).append(">");
            }
            sbd.append(")");
        }
        sbd.append(" time(").append(ms).append("ms)");
        if (U.isNotBlank(responseHeaders)) {
            sbd.append(", response headers(");
            for (String name : responseHeaders.names()) {
                sbd.append("<").append(name).append(" : ").append(responseHeaders.get(name)).append(">");
            }
            sbd.append(")");
        }
        if (U.isNotBlank(result)) {
            // 如果长度大于 6000 就只输出前 200 个字符
            if (result.length() > 6000) {
                result = result.substring(0, 200) + " ...";
            }
            sbd.append(", return(").append(result).append(")");
        } else {
            sbd.append(" return null");
        }
        return sbd.toString();
    }
    /** 发起 http 请求 */
    private static String handleRequest(String url, Request.Builder builder, String params) {
        url = handleEmptyScheme(url);
        Request request = builder.url(url).build();
        String method = request.method();

        long start = System.currentTimeMillis();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response != null) {
                ResponseBody body = response.body();
                if (body != null) {
                    String result = body.string();
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        Headers requestHeaders = request.headers();
                        Headers responseHeaders = response.headers();
                        String log = collectContext(start, method, url, params, requestHeaders, responseHeaders, result);
                        LogUtil.ROOT_LOG.info(log);
                    }
                    return result;
                }
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("(%s %s) exception", method, url), e);
            }
        }
        return null;
    }


    /** 用 get 方式请求 url 并将响应结果保存指定的文件 */
    public static void download(String url, String file) {
        url = handleEmptyScheme(url);
        Request request = new Request.Builder().url(url).build();

        long start = System.currentTimeMillis();
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            if (response != null) {
                ResponseBody body = response.body();
                if (body != null) {
                    Files.write(body.bytes(), new File(file));
                    if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                        long ms = (System.currentTimeMillis() - start);
                        LogUtil.ROOT_LOG.info("download ({}) to file({}) success, time({}ms)", url, file, ms);
                    }
                }
            }
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("download (%s) to file(%s) exception", url, file), e);
            }
        }
    }
}
