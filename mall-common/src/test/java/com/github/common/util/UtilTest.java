package com.github.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilTest {

    @Test
    public void encode() {
        Assertions.assertEquals("", U.urlEncode(null));
        Assertions.assertEquals("", U.urlEncode(""));

        String source = "name=中文&id=123&n=1+2   &n=2";
        String encode = U.urlEncode(source);
        Assertions.assertNotEquals(source, encode);
        Assertions.assertEquals(source, U.urlDecode(encode));
    }

    @Test
    public void chinese() {
        Assertions.assertFalse(U.checkChinese(null));
        Assertions.assertFalse(U.checkChinese(""));
        Assertions.assertFalse(U.checkChinese("wqiroewfds123$%^&*("));

        Assertions.assertTrue(U.checkChinese("wqiroewfds中123$%^&*("));
    }

    @Test
    public void phone() {
        Assertions.assertFalse(U.checkPhone(null));
        Assertions.assertFalse(U.checkPhone(""));
        Assertions.assertFalse(U.checkPhone("131-1234-5678"));
        Assertions.assertFalse(U.checkPhone("131 1234 5678"));
        Assertions.assertFalse(U.checkPhone("131-1234 5678"));
        Assertions.assertFalse(U.checkPhone("1311234678"));

        Assertions.assertTrue(U.checkPhone("13112345678"));
        Assertions.assertTrue(U.checkPhone("12112345678"));
    }

    @Test
    public void image() {
        Assertions.assertFalse(U.checkImage(null));
        Assertions.assertFalse(U.checkImage(""));
        Assertions.assertFalse(U.checkImage("/tmp/image/fdwqrewqiofds.giff"));
        Assertions.assertFalse(U.checkImage("afdwruewqrewq.abc"));

        Assertions.assertTrue(U.checkImage("/tmp/ufio1u8231/abc.png"));
        Assertions.assertTrue(U.checkImage("http://abc.xyz.com/uire4ui231.jpg"));
        Assertions.assertTrue(U.checkImage("D:\\image\\中.bmp"));
        Assertions.assertTrue(U.checkImage(".bmp"));
    }

    @Test
    public void email() {
        Assertions.assertFalse(U.checkEmail(null));
        Assertions.assertFalse(U.checkEmail(""));
        Assertions.assertFalse(U.checkEmail("1$%^&*23-iurew@xyz.13s-rew.com"));
        Assertions.assertFalse(U.checkEmail("-123@xyz.com"));

        Assertions.assertTrue(U.checkEmail("abc-xyz@126.com"));
        Assertions.assertTrue(U.checkEmail("abc@126.com"));
        Assertions.assertTrue(U.checkEmail("10010@qq.com"));
        Assertions.assertTrue(U.checkEmail("_abc-def@123-hij.uvw_xyz.com"));
        Assertions.assertTrue(U.checkEmail("123-iurew@xyz.13s-rew.com"));
    }
}
