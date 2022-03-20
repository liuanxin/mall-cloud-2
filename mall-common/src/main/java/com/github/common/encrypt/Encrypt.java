package com.github.common.encrypt;

import com.github.common.encrypt.jwt.JWTExpiredException;
import com.github.common.encrypt.jwt.JWTSigner;
import com.github.common.encrypt.jwt.JWTVerifier;
import com.github.common.exception.ForbiddenException;
import com.github.common.util.U;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.Data;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
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

    // 也不是很懂为什么 guava 已经发布了这么多的版本却还在 Beta, 这个注解用来抑制 idea 的警告
    @SuppressWarnings("UnstableApiUsage")
    private static final HashFunction HASH_FUN = Hashing.murmur3_32_fixed();
    /** 62 进制(0-9, a-z, A-Z), 64 进制则再加上 _ 和 @ */
    private static final char[] HEX = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    };
    private static final int HEX_LEN = HEX.length;

    /** 使用 base64 编码 */
    public static String base64Encode(String src) {
        return new String(base64Encode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
    public static byte[] base64Encode(byte[] src) {
        return Base64.getEncoder().encode(src);
    }
    /** 使用 base64 解码 */
    public static String base64Decode(String src) {
        return new String(base64Decode(src.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }
    public static byte[] base64Decode(byte[] src) {
        return Base64.getDecoder().decode(src);
    }

    /** 使用 aes 加密(使用默认密钥) */
    public static String aesEncode(String data) {
        return aesEncode(data, AES_SECRET);
    }
    /** 使用 aes 加密 */
    public static String aesEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", AES));
        }
        if (secretKey.length() != AES_LEN) {
            throw new RuntimeException(String.format("%s 加密时, 密钥必须是 %s 位", AES, AES_LEN));
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secretKey.getBytes(UTF8), AES));
            return binary2Hex(cipher.doFinal(data.getBytes(UTF8)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", AES, data, secretKey), e);
        }
    }
    /** 使用 aes 解密(使用默认密钥) */
    public static String aesDecode(String data) {
        return aesDecode(data, AES_SECRET);
    }
    /** 使用 aes 解密 */
    public static String aesDecode(String data, String secretKey) {
        if (U.isBlank(data)) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", AES));
        }
        if (secretKey.length() != AES_LEN) {
            throw new RuntimeException(String.format("%s 解密时, 密钥必须是 %s 位", AES, AES_LEN));
        }
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secretKey.getBytes(UTF8), AES));
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", AES, data, secretKey), e);
        }
    }

    /** 使用 des 加密(使用默认密钥) */
    public static String desEncode(String data) {
        return desEncode(data, DES_SECRET);
    }
    /** 使用 des 加密 */
    public static String desEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", DES));
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(String.format("%s 加密时, 密钥必须是 %s 位", DES, DES_LEN));
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(UTF8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new SecureRandom());
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", DES, data, secretKey), e);
        }
    }
    /** 使用 des 解密(使用默认密钥) */
    public static String desDecode(String data) {
        return desDecode(data, DES_SECRET);
    }
    /** 使用 des 解密 */
    public static String desDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", DES));
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(String.format("%s 解密时, 密钥必须是 %s 位", DES, DES_LEN));
        }
        try {
            DESKeySpec desKey = new DESKeySpec(secretKey.getBytes(UTF8));
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES);
            cipher.init(Cipher.DECRYPT_MODE, secretkey, new SecureRandom());
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", DES, data, secretKey), e);
        }
    }

    /** 使用 DES/CBC/PKCS5Padding 加密(使用默认密钥) */
    public static String desCbcEncode(String data) {
        return desCbcEncode(data, DES_SECRET);
    }
    /** 使用 DES/CBC/PKCS5Padding 加密 */
    public static String desCbcEncode(String data, String secretKey) {
        if (data == null) {
            throw new RuntimeException(String.format("空无需使用 %s 加密", DES_CBC_PKCS5PADDING));
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(String.format("%s 加密时, 密钥必须是 %s 位", DES_CBC_PKCS5PADDING, DES_LEN));
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(UTF8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey secretkey = SecretKeyFactory.getInstance(DES).generateSecret(desKey);

            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.ENCRYPT_MODE, secretkey, new IvParameterSpec(secretKeyBytes));
            return binary2Hex(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 加密(%s)密钥(%s)时异常", DES_CBC_PKCS5PADDING, data, secretKey), e);
        }
    }
    /** 使用 DES/CBC/PKCS5Padding 解密(使用默认密钥) */
    public static String desCbcDecode(String data) {
        return desCbcDecode(data, DES_SECRET);
    }
    /** 使用 DES/CBC/PKCS5Padding 解密 */
    public static String desCbcDecode(String data, String secretKey) {
        if (data == null || data.trim().length() == 0) {
            throw new RuntimeException(String.format("空无需使用 %s 解密", DES_CBC_PKCS5PADDING));
        }
        if (secretKey.length() != DES_LEN) {
            throw new RuntimeException(String.format("%s 解密时, 密钥必须是 %s 位", DES_CBC_PKCS5PADDING, DES_LEN));
        }
        try {
            byte[] secretKeyBytes = secretKey.getBytes(UTF8);
            DESKeySpec desKey = new DESKeySpec(secretKeyBytes);
            SecretKey key = SecretKeyFactory.getInstance(DES).generateSecret(desKey);
            Cipher cipher = Cipher.getInstance(DES_CBC_PKCS5PADDING);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(secretKeyBytes));
            return new String(cipher.doFinal(hex2Binary(data)), UTF8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 解密(%s)密钥(%s)时异常", DES_CBC_PKCS5PADDING, data, secretKey), e);
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
            pair.setPublicKey(new String(base64Encode(keyPair.getPublic().getEncoded()), UTF8));
            pair.setPrivateKey(new String(base64Encode(keyPair.getPrivate().getEncoded()), UTF8));
            return pair;
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 生成 %s 位的密钥对时异常", RSA, keyLength), e);
        }
    }
    /** 使用 rsa 的公钥加密 */
    public static String rsaEncode(String publicKey, String data) {
        try {
            byte[] keyBytes = base64Decode(publicKey.getBytes(UTF8));
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            PublicKey key = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encodeBytes = cipher.doFinal(data.getBytes(UTF8));

            return new String(base64Encode(encodeBytes), UTF8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 基于公钥(%s)加密(%s)时异常", RSA, publicKey, data), e);
        }
    }
    /** 使用 rsa 的私钥解密 */
    public static String rsaDecode(String privateKey, String data) {
        try {
            byte[] keyBytes = base64Decode(privateKey.getBytes(UTF8));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA);
            PrivateKey key = keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodeBytes = cipher.doFinal(base64Decode(data.getBytes(UTF8)));

            return new String(decodeBytes, UTF8);
        } catch (Exception e) {
            throw new RuntimeException(String.format("用 %s 基于私钥(%s)解密(%s)时异常", RSA, privateKey, data), e);
        }
    }


    /**
     * bcrypt 慢加密
     *
     * @param password 原密码
     * @return 加密后的密码
     */
    public static String bcryptEncode(String password) {
        return BCrypt.encrypt(password, BCrypt.genSalt());
    }
    /**
     * 验证密码是否相同
     *
     * @param password 原密码
     * @param encryptPass 加密后的密码. 60 位
     * @return 如果加密后相同, 则返回 true
     */
    public static boolean checkBcrypt(String password, String encryptPass) {
        if (encryptPass == null || encryptPass.length() == 0) {
            return false;
        }

        try {
            return encryptPass.equals(BCrypt.encrypt(password, encryptPass));
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * 验证密码是否不相同
     *
     * @param password 原密码
     * @param encryptPass 加密后的密码. 60 位
     * @return 如果加密后不相同, 则返回 true
     */
    public static boolean checkNotBcrypt(String password, String encryptPass) {
        return !checkBcrypt(password, encryptPass);
    }


    /** 使用 jwt 将 map 进行编码并使用 aes 加密 */
    public static String jwtEncode(Map<String, Object> map) {
        return aesEncode(JWT_SIGNER.sign(map));
    }
    /** 将 map 设置过期时间且进行 jwt 编码并使用 aes 加密 */
    public static String jwtEncode(Map<String, Object> map, long time, TimeUnit unit) {
        map.put(JWTVerifier.EXP, System.currentTimeMillis() + unit.toMillis(time));
        return jwtEncode(map);
    }
    /** 使用 aes 解密并解码 jwt 及验证过期和数据完整性, 解码异常 或 数据已过期 或 验证失败 则抛出未登录异常 */
    public static Map<String, Object> jwtDecode(String data) {
        if (U.isBlank(data)) {
            throw new ForbiddenException("数据有误, 请重新登录");
        }

        try {
            String jwt = aesDecode(data);
            return JWT_VERIFIER.verify(jwt);
        } catch (JWTExpiredException e) {
            throw new ForbiddenException("登录已过期, 请重新登录", e);
        } catch (Exception e) {
            throw new ForbiddenException(String.format("数据(%s)验证失败, 请重新登录", data), e);
        }
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

    /**
     * <pre>
     * 生成短地址: 长度大于 6 则用 Murmur3_32Hash 算法来生成 hash 并返回其 62 进制数(0-9 + a-z + A-Z)
     *
     * getShortString("user/info?id=123&name=中文&sex=0&province=广东") ==> 44y34N
     * 这样可以将 { http://abc.xyz.com/user/info?id=123&name=中文&sex=0&province=广东 } 缩短成 { http://t.io/44y34N }
     * </pre>
     */
    public static String getShortString(String src) {
        if (U.isBlank(src)) {
            return null;
        } else if (src.length() < 6) {
            return src;
        } else {
            StringBuilder sbd = new StringBuilder();
            long pad = Math.abs(HASH_FUN.hashBytes(src.getBytes(StandardCharsets.UTF_8)).padToLong());
            if (pad < HEX_LEN) {
                sbd.append(HEX[(int) pad]);
            } else {
                long t = pad;
                while (t > HEX_LEN) {
                    int p = (int) (t % HEX_LEN);
                    t = t / HEX_LEN;
                    sbd.insert(0, HEX[p]);
                }
                sbd.insert(0, HEX[(int) t]);
            }
            return sbd.toString();
        }
    }

    /** 生成 md5 值(16 位) */
    public static String to16Md5(String src) {
        return toMd5(src).substring(8, 24);
    }
    /** 生成 md5 值(32 位) */
    public static String toMd5(String src) {
        return toHash(src, "md5");
    }
    /** 生成 sha-1 值(40 位) */
    public static String toSha1(String src) {
        return toHash(src, "sha-1");
    }
    /** 生成 sha-224 值(56 位) */
    public static String toSha224(String src) {
        return toHash(src, "sha-224");
    }
    /** 生成 sha-256 值(64 位) */
    public static String toSha256(String src) {
        return toHash(src, "sha-256");
    }
    /** 生成 sha-384 值(96 位) */
    public static String toSha384(String src) {
        return toHash(src, "sha-384");
    }
    /** 生成 sha-512 值(128 位) */
    public static String toSha512(String src) {
        return toHash(src, "sha-512");
    }
    private static String toHash(String src, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(src.getBytes());
            return binary2Hex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法给(%s)生成 %s 值", src, algorithm), e);
        }
    }

    /** 生成文件的 md5 值(32 位) */
    public static String toMd5File(String file) {
        return toHashFile(file, "md5");
    }
    /** 生成文件的 sha-1 值(40 位) */
    public static String toSha1File(String file) {
        return toHashFile(file, "sha-1");
    }
    /** 生成文件的 sha-224 值(56 位) */
    public static String toSha224File(String file) {
        return toHashFile(file, "sha-224");
    }
    /** 生成文件的 sha-256 值(64 位) */
    public static String toSha256File(String file) {
        return toHashFile(file, "sha-256");
    }
    /** 生成文件的 sha-384 值(96 位) */
    public static String toSha384File(String file) {
        return toHashFile(file, "sha-384");
    }
    /** 生成文件的 sha-512 值(128 位) */
    public static String toSha512File(String file) {
        return toHashFile(file, "sha-512");
    }
    private static String toHashFile(String file, String algorithm) {
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            int len, count = 1024;
            byte[] buffer = new byte[count];
            while ((len = in.read(buffer, 0, count)) != -1) {
                md.update(buffer, 0, len);
            }
            return binary2Hex(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法生成文件(%s)的 %s 值", file, algorithm), e);
        }
    }

    /** 基于密钥生成 hmac-md5 值(32 位) */
    public static String toHmacMd5(String src, String secret) {
        return toHmacHash(src, "HmacMD5", secret);
    }
    /** 基于密钥生成 hmac-sha-1 值(40 位) */
    public static String toHmacSha1(String src, String secret) {
        return toHmacHash(src, "HmacSHA1", secret);
    }
    /** 基于密钥生成 hmac-sha-224 值(56 位) */
    public static String toHmacSha224(String src, String secret) {
        return toHmacHash(src, "HmacSHA224", secret);
    }
    /** 基于密钥生成 hmac-sha-256 值(64 位) */
    public static String toHmacSha256(String src, String secret) {
        return toHmacHash(src, "HmacSHA256", secret);
    }
    /** 基于密钥生成 hmac-sha-384 值(96 位) */
    public static String toHmacSha384(String src, String secret) {
        return toHmacHash(src, "HmacSHA384", secret);
    }
    /** 基于密钥生成 hmac-sha-512 值(128 位) */
    public static String toHmacSha512(String src, String secret) {
        return toHmacHash(src, "HmacSHA512", secret);
    }
    private static String toHmacHash(String src, String algorithm, String secret) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(), algorithm));
            return binary2Hex(mac.doFinal(src.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(String.format("无法基于(%s)给(%s)生成 %s 值", secret, src, algorithm), e);
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
