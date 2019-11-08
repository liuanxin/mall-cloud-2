package com.github.common.encrypt;

import com.github.common.util.A;
import org.apache.commons.lang3.math.NumberUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EncryptTest {

    private static final String SOURCE = "password--$%^&*()我中文easy~_+-321 123=/.,";

    @Test
    public void aesCheck() {
        String encode = Encrypt.aesEncode(SOURCE);
        System.out.println(encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.aesDecode(encode);
        System.out.println(decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void desCheck() {
        String key = "12345678";
        String abc = Encrypt.desEncode("abc", key);
        System.out.println(abc);
        String dec = Encrypt.desDecode(abc, key);
        System.out.println(dec);

        String encode = Encrypt.desEncode(SOURCE);
        System.out.println("des: " + encode);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.desDecode(encode);
        System.out.println("des: " + decode);
        Assert.assertEquals(SOURCE, decode);

        encode = Encrypt.desCbcEncode(SOURCE);
        System.out.println("des/cbc: " + encode);
        Assert.assertTrue(encode.length() > 0);

        decode = Encrypt.desCbcDecode(encode);
        System.out.println("des/cbc: " + decode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void rsaCheck() {
        Encrypt.RsaPair pair = Encrypt.genericRsaKeyPair(1024);
        System.out.println("公: " + pair.getPublicKey());
        System.out.println("私: " + pair.getPrivateKey());

        String encode = Encrypt.rsaEncode(pair.getPublicKey(), SOURCE);
        System.out.println("密: " + encode);
        System.out.println("解: " + Encrypt.rsaDecode(pair.getPrivateKey(), encode));

        System.out.println("\n\n");

        pair = Encrypt.genericRsaKeyPair(2048);
        System.out.println("公: " + pair.getPublicKey());
        System.out.println("私: " + pair.getPrivateKey());

        encode = Encrypt.rsaEncode(pair.getPublicKey(), SOURCE);
        System.out.println("密: " + encode);
        System.out.println("解: " + Encrypt.rsaDecode(pair.getPrivateKey(), encode));
    }

    @Test
    public void jwtCheck() {
        String encode = Encrypt.jwtEncode(A.maps(
                "id", 123,
                "name", System.currentTimeMillis()
        ));
        System.out.println(encode);
        Assert.assertTrue(encode.length() > 0);

        Map<String, Object> decode = Encrypt.jwtDecode(encode);
        Assert.assertEquals(123, decode.get("id"));
        Assert.assertTrue(System.currentTimeMillis() > NumberUtils.toLong(decode.get("name").toString()));


        encode = Encrypt.jwtEncode(A.maps("id", 123), 2L, TimeUnit.SECONDS);
        Assert.assertTrue(encode.length() > 0);

        decode = Encrypt.jwtDecode(encode);
        Assert.assertEquals(123, decode.get("id"));


        encode = Encrypt.jwtEncode(A.maps("id", 123), 10L, TimeUnit.MILLISECONDS);
        Assert.assertTrue(encode.length() > 0);

        try {
            Thread.sleep(11L);
        } catch (InterruptedException ignore) {
        }

        decode = Encrypt.jwtDecode(encode);
        Assert.assertTrue(A.isEmpty(decode));
    }

    @Test
    public void base64Test() {
        String encode = Encrypt.base64Encode(SOURCE);
        Assert.assertTrue(encode.length() > 0);

        String decode = Encrypt.base64Decode(encode);
        Assert.assertEquals(SOURCE, decode);
    }

    @Test
    public void bcryptTest() {
        String encode = Encrypt.bcryptEncode(SOURCE);
        Assert.assertTrue(encode.length() > 0);

        String encode2 = Encrypt.bcryptEncode(SOURCE);
        // 两次密码的值不同
        Assert.assertNotEquals(encode, encode2);

        // 加一个空格, 密码就不同了
        Assert.assertTrue(Encrypt.checkNotBcrypt(SOURCE + " ", encode));

        Assert.assertTrue(Encrypt.checkBcrypt(SOURCE, encode));
        Assert.assertTrue(Encrypt.checkBcrypt(SOURCE, encode2));
    }

    @Test
    public void rc4Test() {
        String encode = Encrypt.rc4Encode(SOURCE);
        System.out.println(encode);

        String decode = Encrypt.rc4Decode(encode);
        System.out.println(decode);
        Assert.assertTrue(SOURCE.equals(decode));
    }

    @Test
    public void digestTest() {
        String encode = Encrypt.to16Md5(SOURCE);
        Assert.assertTrue(encode.length() == 16);

        encode = Encrypt.toMd5(SOURCE);
        Assert.assertTrue(encode.length() == 32);

        encode = Encrypt.toSha1(SOURCE);
        Assert.assertTrue(encode.length() == 40);

        encode = Encrypt.toSha224(SOURCE);
        Assert.assertTrue(encode.length() == 56);

        encode = Encrypt.toSha256(SOURCE);
        Assert.assertTrue(encode.length() == 64);

        encode = Encrypt.toSha384(SOURCE);
        Assert.assertTrue(encode.length() == 96);

        encode = Encrypt.toSha512(SOURCE);
        Assert.assertTrue(encode.length() == 128);
    }

    @Test
    public void hmacTest() {
        String k = "192006250b4c09247ec02edce69f6a2d";
        String p = "appid=wxd930ea5d5a258f4f&body=test&device_info=1000&mch_id=10000100&nonce_str=ibuaiVcKdpRxkhJA";
        String str = p + "&key=" + k;

        String encode = Encrypt.toHmacMd5(str, k);
        Assert.assertTrue(encode.length() == 32);

        encode = Encrypt.toHmacSha1(str, k);
        Assert.assertTrue(encode.length() == 40);

        encode = Encrypt.toHmacSha224(str, k);
        Assert.assertTrue(encode.length() == 56);

        encode = Encrypt.toHmacSha256(str, k);
        Assert.assertTrue(encode.length() == 64);

        encode = Encrypt.toHmacSha384(str, k);
        Assert.assertTrue(encode.length() == 96);

        encode = Encrypt.toHmacSha512(str, k);
        Assert.assertTrue(encode.length() == 128);
    }
}
