package com.github.common.encrypt;

import com.github.common.encrypt.jwt.JWTExpiredException;
import com.github.common.encrypt.jwt.JWTSigner;
import com.github.common.encrypt.jwt.JWTVerifier;
import com.github.common.encrypt.jwt.JWTVerifyException;
import com.github.common.util.LogUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** aes 加密解密, jwt 加密解密, base64 编码解码, md5、sha-1、sha-224、sha-256、sha-384、sha-512 加密算法 */
public final class Encrypt {

    /** 加密解密算法 */
    private static final String ALGORITHM = "AES";
    /** aes 加解密时, 长度必须为 16 位的密钥 */
    private static final byte[] AES_SECRET = "&gAe#sEn!cr*yp^t".getBytes(StandardCharsets.UTF_8);

    private static final String SECRET_KEY = "*g0$%Te#nr&y^pOt";
    private static final JWTSigner JWT_SIGNER = new JWTSigner(SECRET_KEY);
    private static final JWTVerifier JWT_VERIFIER = new JWTVerifier(SECRET_KEY);

    /** 使用 aes 加密 */
    public static String aesEncode(String data) {
        if (data == null) {
            return "空值无法加密";
        }
        if (AES_SECRET.length != 16) {
            return "密钥必须是 16 位";
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES_SECRET, ALGORITHM));
            byte[] bytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            // 二进制转换成十六进制字符
            StringBuilder sbd = new StringBuilder();
            for (byte bt : bytes) {
                String hex = (Integer.toHexString(bt & 0XFF));
                if (hex.length() == 1) {
                    sbd.append("0");
                }
                sbd.append(hex);
            }
            return sbd.toString();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("使用 " + ALGORITHM + "(" + data + ")加密失败", e);
            }
            throw new RuntimeException(ALGORITHM + "(" + data + ")加密失败");
        }
    }

    /** 使用 aes 解密 */
    public static String aesDecode(String data) {
        if (data == null || data.trim().length() == 0) {
            return "空值无法解密";
        }
        if (AES_SECRET.length != 16) {
            return "密钥必须是 16 位";
        }
        try {
            // 二进制转成十六进制
            byte[] bt = data.getBytes(StandardCharsets.UTF_8);
            if ((bt.length % 2) != 0) {
                return "非偶数位的值无法解密";
            }

            byte[] bytes = new byte[bt.length / 2];
            for (int n = 0; n < bt.length; n += 2) {
                String item = new String(bt, n, 2);
                bytes[n / 2] = (byte) Integer.parseInt(item, 16);
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(AES_SECRET, ALGORITHM));
            return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug(ALGORITHM + "(" + data + ")解密异常", e);
            }
            throw new RuntimeException(ALGORITHM + "(" + data + ")解密时异常");
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
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("使用 jwt 解密(" + data + ")时, 数据已过期", e);
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException |
                SignatureException | JWTVerifyException e) {
            if (LogUtil.ROOT_LOG.isTraceEnabled()) {
                LogUtil.ROOT_LOG.trace("使用 jwt 解密(" + data + ")失败", e);
            }
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("使用 jwt 解密(" + data + ")异常", e);
            }
        }
        return Collections.emptyMap();
    }

    /** 使用 rc4 加解密, 如果是密文调用此方法将返回明文 */
    public static String rc4(String input, String key) {
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
        return new String(Base64.getEncoder().encode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }

    /** 使用 base64 解码 */
    public static String base64Decode(String src) {
        return new String(Base64.getDecoder().decode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
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
            byte[] byteArray = md.digest();

            StringBuilder sbd = new StringBuilder();
            for (byte b : byteArray) {
                sbd.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sbd.toString();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("无法给(" + src + ")生成(" + method + ")摘要", e);
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

            StringBuilder sbd = new StringBuilder();
            for (byte b : md.digest()) {
                sbd.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            return sbd.toString();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                LogUtil.ROOT_LOG.debug("无法生成 md5", e);
            }
            throw new RuntimeException("无法生成 md5");
        }
    }
}
