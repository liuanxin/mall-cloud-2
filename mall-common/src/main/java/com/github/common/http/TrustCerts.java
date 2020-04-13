package com.github.common.http;

import com.github.common.util.LogUtil;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

class TrustCerts {

    static final X509TrustManager INSTANCE = new TrustCertsManager();
    /** 请求 https 时无视 ssl 证书 */
    static final SSLContext IGNORE_SSL_CONTEXT = createIgnoreVerifySSL();
    /** 请求 https 时使用本地的 ssl 文件证书 */
    static final SSLContext FILE_SSL_CONTEXT = createFileVerifySSL();

    static class TrustCertsManager implements X509TrustManager {
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

    private static final String FILE_JKS = "/xxx/yyy/file.jks";
    /** store-pass 用来访问密码库 */
    private static final String KEY_STORE_PASS = "key-store-pass";
    /** key-pass 用来访问密码库中密钥对的私钥 */
    private static final String KEY_PASS = "key-pass";

    static SSLContext createFileVerifySSL() {
        try (InputStream keyStoreStream = new FileInputStream(FILE_JKS)) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(keyStoreStream, KEY_STORE_PASS.toCharArray());
            return SSLContexts.custom().loadKeyMaterial(keyStore, KEY_PASS.toCharArray()).build();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("设置本地 ssl 证书异常", e);
            }
            return null;
        }
    }
}
