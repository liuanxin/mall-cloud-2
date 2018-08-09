package com.github.common.http;

import com.github.common.util.LogUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class TrustAllCerts implements X509TrustManager {

    static final TrustAllCerts INSTANCE = new TrustAllCerts();
    static final SSLContext SSL_CONTEXT = createIgnoreVerifySSL();

    @Override
    public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate,
                                   String paramString) throws CertificateException {
    }
    @Override
    public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate,
                                   String paramString) throws CertificateException {
    }
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    private static SSLContext createIgnoreVerifySSL() {
        try {
            SSLContext sc = SSLContext.getInstance("SSLv3");
            sc.init(null, new TrustManager[] { INSTANCE }, null);
            return sc;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("设置忽略 ssl 证书异常", e);
            }
            return null;
        }
    }
}
