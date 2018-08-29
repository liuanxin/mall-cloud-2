package com.github.common.encrypt;

import com.github.common.encrypt.jwt.JWTExpiredException;
import com.github.common.encrypt.jwt.JWTSigner;
import com.github.common.encrypt.jwt.JWTVerifier;
import com.github.common.encrypt.jwt.JWTVerifyException;
import com.github.common.util.LogUtil;
import lombok.Data;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** AES、DES、DES/CBC/PKCS5Padding、jwt 加密解密, base64 编码解码, md5、sha-1、sha-224、sha-256、sha-384、sha-512 加密算法 */
public final class Encrypt {

    private static final String AES = "AES";
    /** aes 加解密时, 长度必须是 16 位的密钥 */
    private static final String AES_SECRET = "&gAe#sEn!cr*yp^t";
    private static final int AES_LEN = 16;

    private static final String DES = "DES";
    private static final String DES_CBC_PKCS5PADDING = "DES/CBC/PKCS5Padding";
    /** des 加解密时, 长度必须是 8 位的密钥 */
    private static final String DES_SECRET = "%d#*Es^e";
    private static final int DES_LEN = 8;

    private static final String RSA = "RSA";

    private static final String RC4_SECRET_KEY = "^&NK$1j8kO#h=)hU";

    private static final String JWT_SECRET_KEY = "*W0$%Te#nr&y^pOt";
    private static final JWTSigner JWT_SIGNER = new JWTSigner(JWT_SECRET_KEY);
    private static final JWTVerifier JWT_VERIFIER = new JWTVerifier(JWT_SECRET_KEY);

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    /** 使用 aes 加密(使用默认密钥) */
    public static String aesEncode(String data) {
        return aesEncode(data, AES_SECRET);
    }
    /** 使用 aes 加密 */
    public static String aesEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException("空无需使用 " + AES + " 加密");
        }
        if (secretKey.length() != AES_LEN) {
            throw new RuntimeException(AES + " 加密时, 密钥必须是 " + AES_LEN + " 位");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey.getBytes(UTF8), AES));
            return binary2Hex(cipher.doFinal(data.getBytes(UTF8)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(AES + "(" + data + ")加密异常", e);
            }
            throw new RuntimeException(AES + "(" + data + ")加密异常");
        }
    }
    /** 使用 aes 解密(使用默认密钥) */
    public static String aesDecode(String data) {
        return aesDecode(data, AES_SECRET);
    }
    /** 使用 aes 解密 */
    public static String aesDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException("空值无需使用 " + AES + " 解密");
        }
        if (secretKey.length() != AES_LEN) {
            throw new RuntimeException(AES + " 解密时, 密钥必须是 " + AES_LEN + " 位");
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.getBytes(UTF8), AES));
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (BadPaddingException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(AES + "(" + data + ")解密时密钥错误", e);
            }
            throw new RuntimeException(AES + "(" + data + ")解密时密钥错误");
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(AES + "(" + data + ")解密异常", e);
            }
            throw new RuntimeException(AES + "(" + data + ")解密异常");
        }
    }

    /** 使用 des 加密(使用默认密钥) */
    public static String desEncode(String data) {
        return desEncode(data, DES_SECRET);
    }
    /** 使用 des 加密 */
    public static String desEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException("空无需使用 " + DES + " 加密");
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(DES + " 加密时, 密钥必须是 " + DES_LEN + " 位");
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(UTF8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new SecureRandom());
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(AES + "(" + data + ")加密失败", e);
            }
            throw new RuntimeException(AES + "(" + data + ")加密失败");
        }
    }
    /** 使用 des 解密(使用默认密钥) */
    public static String desDecode(String data) {
        return desDecode(data, DES_SECRET);
    }
    /** 使用 des 解密 */
    public static String desDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException("空值无需使用 " + DES + " 解密");
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(DES + " 解密时, 密钥必须是 " + DES_LEN + " 位");
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(UTF8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.DECRYPT_MODE, secretkey, new SecureRandom());
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (BadPaddingException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(DES + "(" + data + ")解密时密钥错误", e);
            }
            throw new RuntimeException(DES + "(" + data + ")解密时密钥错误");
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(DES + "(" + data + ")解密异常", e);
            }
            throw new RuntimeException(DES + "(" + data + ")解密异常");
        }
    }

    /** 使用 DES/CBC/PKCS5Padding 加密(使用默认密钥) */
    public static String desCbcEncode(String data) {
        return desCbcEncode(data, DES_SECRET);
    }
    /** 使用 DES/CBC/PKCS5Padding 加密 */
    public static String desCbcEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException("空无需使用 " + DES_CBC_PKCS5PADDING + " 加密");
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(DES_CBC_PKCS5PADDING + " 加密时, 密钥必须是 " + DES_LEN + " 位");
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(UTF8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new IvParameterSpec(secretKeyBytes));
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(DES_CBC_PKCS5PADDING + "(" + data + ")加密失败", e);
            }
            throw new RuntimeException(DES_CBC_PKCS5PADDING + "(" + data + ")加密失败");
        }
    }
    /** 使用 DES/CBC/PKCS5Padding 解密(使用默认密钥) */
    public static String desCbcDecode(String data) {
        return desCbcDecode(data, DES_SECRET);
    }
    /** 使用 DES/CBC/PKCS5Padding 解密 */
    public static String desCbcDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException("空值无需使用 " + DES_CBC_PKCS5PADDING + " 解密");
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(DES_CBC_PKCS5PADDING + " 解密时, 密钥必须是 " + DES_LEN + " 位");
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(UTF8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey key = SecretKeyFactory.getInstance(DES).generateSecret(desKey);
            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(secretKeyBytes));
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (BadPaddingException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(DES_CBC_PKCS5PADDING + "(" + data + ")解密时密钥错误", e);
            }
            throw new RuntimeException(DES_CBC_PKCS5PADDING + "(" + data + ")解密时密钥错误");
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn(DES_CBC_PKCS5PADDING + "(" + data + ")解密异常", e);
            }
            throw new RuntimeException(DES_CBC_PKCS5PADDING + "(" + data + ")解密时异常");
        }
    }


    @Data
    public static class RsaPair {
        /** 公钥, 发给客户端 */
        private String publicKey;
        /** 私钥, 保存到文件 */
        private String privateKey;
    }
    /** 生成 rsa 的密钥对 */
    public static RsaPair genericRsaKeyPair(int keyLength) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            keyPairGenerator.initialize(keyLength);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            RsaPair pair = new RsaPair();
            pair.setPublicKey(new String(Base64.getEncoder().encode(keyPair.getPublic().getEncoded()), UTF8));
            pair.setPrivateKey(new String(Base64.getEncoder().encode(keyPair.getPrivate().getEncoded()), UTF8));
            return pair;
        } catch (NoSuchAlgorithmException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("RSA(" + keyLength + ")生成密钥对时异常", e);
            }
            throw new RuntimeException("RSA(" + keyLength + ")生成密钥对时异常");
        }
    }
    /** 使用 rsa 的公钥加密 */
    public static String rsaEncode(String publicKey, String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey.getBytes(UTF8));
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            PublicKey key = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encodeBytes = cipher.doFinal(data.getBytes(UTF8));

            return new String(Base64.getEncoder().encode(encodeBytes), UTF8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("RSA(" + data + ")加密失败", e);
            }
            throw new RuntimeException("RSA(" + data + ")加密失败");
        }
    }
    /** 使用 rsa 的私钥解密 */
    public static String rsaDecode(String privateKey, String data) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey.getBytes(UTF8));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            PrivateKey key = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodeBytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes(UTF8)));

            return new String(decodeBytes, UTF8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("RSA(" + data + ")解密失败", e);
            }
            throw new RuntimeException("RSA(" + data + ")解密失败");
        }
    }


    /** 使用 jwt 将 map 加密, 其内部默认使用 HmacSHA256 算法 */
    public static String jwtEncode(Map<String, Object> map) {
        return JWT_SIGNER.sign(map);
    }

    /** 使用 jwt 将 map 加密, 并设置一个过期时间. 其内部默认使用 HmacSHA256 算法 */
    public static String jwtEncode(Map<String, Object> map, long time, TimeUnit unit) {
        map.put(JWTVerifier.EXP, System.currentTimeMillis() + unit.toMillis(time));
        return jwtEncode(map);
    }

    /** 使用 jwt 解密, 其内部默认使用 HmacSHA256 算法 */
    public static Map<String, Object> jwtDecode(String data) {
        try {
            return JWT_VERIFIER.verify(data);
        } catch (JWTExpiredException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("使用 jwt 解密(" + data + ")时, 数据已过期", e);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException |
                SignatureException | JWTVerifyException e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("使用 jwt 解密(" + data + ")失败", e);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("使用 jwt 解密(" + data + ")异常", e);
            }
        }
        return Collections.emptyMap();
    }


    /** 使用 rc4 加密(使用默认密钥) */
    public static String rc4Encode(String input) {
        return rc4Encode(input, RC4_SECRET_KEY);
    }
    /** 使用 rc4 加密 */
    public static String rc4Encode(String input, String key) {
        return base64Encode(rc4(input, key));
    }
    /** 使用 rc4 解密(使用默认密钥) */
    public static String rc4Decode(String input) {
        return rc4Decode(input, RC4_SECRET_KEY);
    }
    /** 使用 rc4 解密 */
    public static String rc4Decode(String input, String key) {
        return rc4(base64Decode(input), key);
    }
    /** 使用 rc4 加解密, 如果是密文调用此方法将返回明文 */
    private static String rc4(String input, String key) {
        int[] iS = new int[256];
        byte[] iK = new byte[256];

        for (int i = 0; i < 256; i++) {
            iS[i] = i;
        }

        for (short i = 0; i < 256; i++) {
            iK[i] = (byte) key.charAt((i % key.length()));
        }

        int j = 0;
        for (int i = 0; i < 255; i++) {
            j = (j + iS[i] + iK[i]) % 256;
            int temp = iS[i];
            iS[i] = iS[j];
            iS[j] = temp;
        }

        int i = 0;
        j = 0;
        char[] iInputChar = input.toCharArray();
        char[] iOutputChar = new char[iInputChar.length];
        for (short x = 0; x < iInputChar.length; x++) {
            i = (i + 1) % 256;
            j = (j + iS[i]) % 256;
            int temp = iS[i];
            iS[i] = iS[j];
            iS[j] = temp;
            int t = (iS[i] + (iS[j] % 256)) % 256;
            int iY = iS[t];
            char iCY = (char) iY;
            iOutputChar[x] = (char) (iInputChar[x] ^ iCY);
        }
        return new String(iOutputChar);
    }

    /** 使用 base64 编码 */
    public static String base64Encode(String src) {
        try {
            return new String(Base64.getEncoder().encode(src.getBytes(UTF8)), UTF8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("base64(" + src + ")编码失败", e);
            }
            throw new RuntimeException("base64 编码失败");
        }
    }

    /** 使用 base64 解码 */
    public static String base64Decode(String src) {
        try {
            return new String(Base64.getDecoder().decode(src.getBytes(UTF8)), UTF8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("base64(" + src + ")解码失败", e);
            }
            throw new RuntimeException("base64 解码失败");
        }
    }


    /** 生成 md5 摘要(16 位) */
    public static String to16Md5(String src) {
        return toMd5(src).substring(8, 24);
    }

    /** 生成 md5 摘要(32 位) */
    public static String toMd5(String src) {
        return toHash(src, "md5");
    }

    /** 生成 sha-1 摘要(40 位) */
    public static String toSha1(String src) {
        return toHash(src, "sha-1");
    }

    /** 生成 sha-224 摘要(56 位) */
    public static String toSha224(String src) {
        return toHash(src, "sha-224");
    }

    /** 生成 sha-256 摘要(64 位) */
    public static String toSha256(String src) {
        return toHash(src, "sha-256");
    }

    /** 生成 sha-384 摘要(96 位) */
    public static String toSha384(String src) {
        return toHash(src, "sha-384");
    }

    /** 生成 sha-512 摘要(128 位) */
    public static String toSha512(String src) {
        return toHash(src, "sha-512");
    }

    private static String toHash(String src, String method) {
        try {
            MessageDigest md = MessageDigest.getInstance(method);
            md.update(src.getBytes());
            return binary2Hex(md.digest());
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("无法给(" + src + ")生成(" + method + ")摘要", e);
            }
            throw new RuntimeException("无法给(" + src + ")生成(" + method + ")摘要");
        }
    }

    public static String toMd5File(String file) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("md5");
            int len, count = 1024;
            byte[] buffer = new byte[count];
            while ((len = in.read(buffer, 0, count)) != -1) {
                md.update(buffer, 0, len);
            }
            return binary2Hex(md.digest());
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isWarnEnabled()) {
                LogUtil.ROOT_LOG.warn("无法生成 md5", e);
            }
            throw new RuntimeException("无法生成 md5");
        }
    }

    /** 二进制 转换成 十六进制字符串 */
    public static String binary2Hex(byte[] bytes) {
        StringBuilder sbd = new StringBuilder();

        for (byte b : bytes) {
            /*
            String hex = (Integer.toHexString(b & 0XFF));
            if (hex.length() == 1) {
                sbd.append("0");
            }
            sbd.append(hex);
            */
            // 上面和下面等同
            sbd.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sbd.toString();
    }
    /** 十六进制字符串 转换成 二进制 */
    public static byte[] hex2Binary(String data) {
        byte[] bt = data.getBytes(UTF8);
        byte[] bytes = new byte[bt.length / 2];
        for (int n = 0; n < bt.length; n += 2) {
            String item = new String(bt, n, 2);
            bytes[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return bytes;
    }
}
