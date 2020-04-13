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

    /**
     * <pre>
     * 见: https://docs.oracle.com/javase/7/docs/technotes/tools/solaris/keytool.html
     *
     * 生成 jks 文件
     * keytool -genkey -alias xxx-jks \
     *   -dname "CN=Tom Cat, OU=Java, O=Oracle, L=SZ, ST=GD, C=CN" \
     *   -keyalg RSA -keystore file.jks -validity 3600 \
     *   -storepass 123456 -keypass abcdef
     *
     * 转换成 pkcs12 标准格式
     * keytool -importkeystore -srcalias xxx-jks -destalias xxx-new-jks \
     *   -srckeystore file.jks -srcstoretype jks -destkeystore file-new.jks -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     *
     * storepass 用来访问密钥库, keypass 用来访问密钥库中具体的密钥. 通常建议保持一致
     * 如果 storepass 和 keypass 是一致的, -importkeystore 时则不需要 -destkeypass 选项, 下同
     *
     *
     * 使用旧的 jks 转换成 pfx 格式(IIS)
     * keytool -importkeystore -srcalias xxx-jks -destalias xxx-pfx \
     *   -srckeystore file.jks -srcstoretype jks -destkeystore file.pfx -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     * 使用新的 jks 转换成 pfx 格式
     * keytool -importkeystore -srcalias xxx-new-jks -destalias xxx-new-pfx \
     *   -srckeystore file-new.jks -srcstoretype pkcs12 -destkeystore file-new.pfx -deststoretype pkcs12 \
     *   -srcstorepass 123456 -srckeypass abcdef -deststorepass 123456 -destkeypass abcdef
     *
     *
     * 将 jks 导出成 crt 和 key 文件(apache 及 nginx), 提取时用 openssl 命令基于 pkcs12 标准格式的文件操作即可
     * 见: https://www.openssl.org/docs/man1.0.2/man1/pkcs12.html
     *
     * 提取 key: openssl pkcs12 -in file-new.jks -nocerts -nodes -out file.key -passin pass:123456
     * 提取 crt: openssl pkcs12 -in file-new.jks -nokeys -out file.crt -passin pass:123456
     * 将 crt 转换成 pem 格式: openssl x509 -in file.crt -out file.pem
     * </pre>
     */
    private static final String FILE_JKS = "/xxx/yyy/file.jks";
    /** store-pass 用来访问密钥库 */
    private static final String KEY_STORE_PASS = "key-store-pass";
    /** key-pass 用来访问密钥库中的密钥 */
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
