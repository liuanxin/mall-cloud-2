package com.github.common.encrypt;

import com.github.common.util.A;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EncryptTest {

    private static final String SOURCE = "password--$%^&*()我中文easy~_+-=/.,";

    @Test
    public void aesCheck() {
        String encode = Encrypt.aesEncode(SOURCE);
        // LogUtil.ROOT_LOG.debug("aes 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.aesDecode(encode);
        // LogUtil.ROOT_LOG.debug("aes 解密: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void jwtCheck() throws Exception {
        String encode = Encrypt.jwtEncode(A.maps(
                "id", "张三",
                "time", System.currentTimeMillis()
        ));
        // LogUtil.ROOT_LOG.debug("jwt 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        Map<String, Object> decode = Encrypt.jwtDecode(encode);
        // LogUtil.ROOT_LOG.debug("jwt 解密: " + decode);
        Assert.assertEquals("张三", decode.get("id"));
        Assert.assertTrue(System.currentTimeMillis() > Long.parseLong(decode.get("time").toString()));


        encode = Encrypt.jwtEncode(A.maps("id", "张三"), 2L, TimeUnit.SECONDS);
        // LogUtil.ROOT_LOG.debug("jwt 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        decode = Encrypt.jwtDecode(encode);
        Assert.assertEquals("张三", decode.get("id"));


        encode = Encrypt.jwtEncode(A.maps("id", "张三"), 1L, TimeUnit.SECONDS);
        // LogUtil.ROOT_LOG.debug("jwt 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        Thread.sleep(1001L);

        decode = Encrypt.jwtDecode(encode);
        Assert.assertTrue(A.isEmpty(decode));
    }

    @Test
    public void base64Test() {
        String encode = Encrypt.base64Encode(SOURCE);
        // LogUtil.ROOT_LOG.debug("base64 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.base64Decode(encode);
        // LogUtil.ROOT_LOG.debug("base64 解密: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void bcryptTest() {
        String encode = BCrypt.encrypt(SOURCE);
        // LogUtil.ROOT_LOG.debug("bcrypt 加密: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String encode2 = BCrypt.encrypt(SOURCE);
        // LogUtil.ROOT_LOG.debug("bcrypt 加密: " + encode2);
        // 两次密码的值不同
        Assert.assertNotEquals(encode, encode2);

        // 加一个空格, 密码就不同了
        Assert.assertTrue(BCrypt.notSame(SOURCE + " ", encode));

        Assert.assertTrue(BCrypt.same(SOURCE, encode));
        Assert.assertTrue(BCrypt.same(SOURCE, encode2));
    }

    @Test
    public void digestTest() {
        String encode = Encrypt.to16Md5(SOURCE);
        // LogUtil.ROOT_LOG.debug("16 位的 md5 加密: " + encode);
        Assert.assertTrue(encode.length() == 16);

        encode = Encrypt.toMd5(SOURCE);
        // LogUtil.ROOT_LOG.debug("md5 加密: " + encode);
        Assert.assertTrue(encode.length() == 32);

        encode = Encrypt.toSha1(SOURCE);
        // LogUtil.ROOT_LOG.debug("sha-1 加密: " + encode);
        Assert.assertTrue(encode.length() == 40);

        encode = Encrypt.toSha224(SOURCE);
        // LogUtil.ROOT_LOG.debug("sha-224 加密: " + encode);
        Assert.assertTrue(encode.length() == 56);

        encode = Encrypt.toSha256(SOURCE);
        // LogUtil.ROOT_LOG.debug("sha-256 加密: " + encode);
        Assert.assertTrue(encode.length() == 64);

        encode = Encrypt.toSha384(SOURCE);
        // LogUtil.ROOT_LOG.debug("sha-384 加密: " + encode);
        Assert.assertTrue(encode.length() == 96);

        encode = Encrypt.toSha512(SOURCE);
        // LogUtil.ROOT_LOG.debug("sha-512 加密:" + encode);
        Assert.assertTrue(encode.length() == 128);
    }
}
