package com.github.common.util;

import com.github.common.Money;
import com.github.common.date.DateUtil;
import com.github.common.exception.*;
import com.github.common.json.JsonUtil;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.regex.Pattern;

/** 工具类 */
public final class U {

    public static final Random RANDOM = new Random();

    /** 本机的 cpu 核心数 */
    public static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    public static final String EMPTY = "";
    public static final String BLANK = " ";
    private static final Pattern MULTI_SPACE_REGEX = Pattern.compile("\\s{2,}");

    private static final String LIKE = "%";

    // 匹配所有手机卡: ^(?:\+?86)?1(?:3\d{3}|5[^4\D]\d{2}|8\d{3}|7[^0129\D](?(?<=4)(?:0\d|1[0-2]|9\d)|\d{2})|9[189]\d{2}|66\d{2})\d{6}$
    /** 匹配所有支持短信功能的号码（手机卡 + 上网卡） */
    private static final String PHONE = "^(?:\\+?86)?1(?:3\\d{3}|5[^4\\D]\\d{2}|8\\d{3}|7[^29\\D](?(?<=4)(?:0\\d|1[0-2]|9\\d)|\\d{2})|9[189]\\d{2}|6[567]\\d{2}|4[579]\\d{2})\\d{6}$";
    /** _abc-def@123-hij.uvw_xyz.com 是正确的, -123@xyz.com 不是 */
    private static final String EMAIL = "^\\w[\\w\\-]*@([\\w\\-]+\\.\\w+)+$";
    /** ico, jpeg, jpg, bmp, png 后缀 */
    private static final String IMAGE = "(?i)^(.*)\\.(ico|jpeg|jpg|bmp|png)$";
    /** IPv4 地址 */
    private static final String IPV4 = "^([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])(\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])){3}$";
    /** 身份证号码 */
    private static final String ID_CARD = "(^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";

    /** 字母 */
    private static final String LETTER = "(?i)[a-z]";
    /** 中文 */
    private static final String CHINESE = "[\\u4e00-\\u9fa5]";
    /** 是否是移动端: https://gist.github.com/dalethedeveloper/1503252 */
    private static final String MOBILE = "(?i)Mobile|iP(hone|od|ad)|Android|BlackBerry|Blazer|PSP|UCWEB|IEMobile|Kindle|NetFront|Silk-Accelerated|(hpw|web)OS|Fennec|Minimo|Opera M(obi|ini)|Dol(f|ph)in|Skyfire|Zune";
    /** 是否是 iOS 端 */
    private static final String IOS = "(?i)iP(hone|od|ad)";
    /** 是否是 android 端 */
    private static final String ANDROID = "(?i)Mobile|Android";
    /** 是否是 pc 端 */
    private static final String PC = "(?i)AppleWebKit|Mozilla|Chrome|Safari|MSIE|Windows NT";
    /** 是否是本地 ip */
    private static final String LOCAL = "(?i)127.0.0.1|localhost|::1|0:0:0:0:0:0:0:1";

    /** 生成指定位数的随机数: 纯数字 */
    public static String random(int length) {
        if (length <= 0) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sbd.append(RANDOM.nextInt(10));
        }
        return sbd.toString();
    }

    private static final String TMP = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    /** 生成指定位数的随机数: 数字和字母 */
    public static String randomLetterAndNumber(int length) {
        if (length <= 0) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sbd.append(TMP.charAt(RANDOM.nextInt(TMP.length())));
        }
        return sbd.toString();
    }

    // ========== enum ==========
    /**
     * 获取枚举中的值, 先匹配 name, 再匹配 getCode(数字), 再匹配 getValue(中文), 都匹配不上则返回 null
     *
     * @param clazz 枚举的类信息
     * @param obj 要匹配的值
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Enum> E toEnum(Class<E> clazz, Object obj) {
        if (isNotBlank(obj)) {
            E[] constants = clazz.getEnumConstants();
            if (constants != null && constants.length > 0) {
                String source = obj.toString().trim();
                for (E em : constants) {
                    // 如果传递过来的是枚举名, 且能匹配上则返回
                    if (source.equalsIgnoreCase(em.name())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getCode(数字) 相同则返回
                    Object code = getMethod(em, "getCode");
                    if (isNotBlank(code) && source.equalsIgnoreCase(code.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 getValue(中文) 相同则返回
                    Object value = getMethod(em, "getValue");
                    if (isNotBlank(value) && source.equalsIgnoreCase(value.toString().trim())) {
                        return em;
                    }

                    // 如果传递过来的值跟枚举的 ordinal(数字. 表示枚举所在的索引) 相同则返回
                    // if (source.equalsIgnoreCase(String.valueOf(em.ordinal()))) return em;
                }
            }
        }
        return null;
    }

    private static final String ENUM_CODE = "code";
    private static final String ENUM_VALUE = "value";
    /**
     * <pre>
     * 序列化枚举, 如以下示例
     *
     * public enum Gender {
     *   Male(0, "男"), Female(1, "女");
     *   int code;
     *   String value;
     *
     *   Gender(int code, String value) {
     *     this.code = code;
     *     this.value = value;
     *   }
     *   // get etc...
     *
     *   &#064;JsonValue
     *   public Map<String, String> serializer() {
     *     return <span style="color:red">serializerEnum(code, value);</span>
     *   }
     *   &#064;JsonCreator
     *   public static Gender deserializer(Object obj) {
     *     return enumDeserializer(obj, Gender.class);
     *   }
     * }
     *
     * Gender.Male 在序列化时将返回 { "code": 0, "value": "男" } 其中 code 用来交互, value 用来显示
     * 反序列化时, 0、男、{ "code": 0, "value": "男" } 都可以反序列化成 Gender.Male
     * </pre>
     */
    public static Map<String, String> serializerEnum(int code, String value) {
        return A.maps(ENUM_CODE, code, ENUM_VALUE, value);
    }
    /**
     * <pre>
     * 枚举反序列化, 如以下示例
     *
     * public enum Gender {
     *   Male(0, "男"), Female(1, "女");
     *   int code;
     *   String value;
     *
     *   Gender(int code, String value) {
     *     this.code = code;
     *     this.value = value;
     *   }
     *   // get etc...
     *
     *   &#064;JsonValue
     *   public Map<String, String> serializer() {
     *     return serializerEnum(code, value);
     *   }
     *   &#064;JsonCreator
     *   public static Gender deserializer(Object obj) {
     *     return <span style="color:red">enumDeserializer(obj, Gender.class);</span>
     *   }
     * }
     *
     * Gender.Male 在序列化时将返回 { "code": 0, "value": "男" } code 用来交互, value 用来显示
     * 而 0、男、{ "code": 0, "value": "男" } 也都可以反序列化成 Gender.Male
     * </pre>
     */
    @SuppressWarnings("rawtypes")
    public static <E extends Enum> E enumDeserializer(Object obj, Class<E> enumClass) {
        if (isBlank(obj)) {
            return null;
        }

        Object tmp = null;
        if (obj instanceof Map) {
            tmp = getEnumInMap((Map) obj);
        } else {
            String tmpStr = obj.toString().trim();
            if (tmpStr.startsWith("{") && tmpStr.endsWith("}")) {
                tmp = getEnumInMap(JsonUtil.toObjectNil(obj.toString(), Map.class));
            }
        }

        if (isBlank(tmp)) {
            tmp = obj;
        }
        return toEnum(enumClass, tmp);
    }
    @SuppressWarnings("rawtypes")
    private static Object getEnumInMap(Map map) {
        if (A.isNotEmpty(map)) {
            Object tmp = map.get(ENUM_CODE);
            if (isBlank(tmp)) {
                tmp = map.get(ENUM_VALUE);
            }
            return tmp;
        } else {
            return null;
        }
    }
    // ========== enum ==========


    // ========== number ==========
    /** 传入的数不为 null 且 大于 0 就返回 true */
    public static boolean greater0(Number obj) {
        return obj != null && obj.doubleValue() > 0;
    }
    /** 传入的数为 null 或 小于等于 0 就返回 true */
    public static boolean less0(Number obj) {
        return !greater0(obj);
    }

    public static int toInt(Object obj) {
        if (isBlank(obj)) {
            return 0;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static long toLong(Object obj) {
        if (isBlank(obj)) {
            return 0L;
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        try {
            return Long.parseLong(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    public static float toFloat(Object obj) {
        if (isBlank(obj)) {
            return 0F;
        }
        if (obj instanceof Number) {
            return ((Number) obj).floatValue();
        }
        try {
            return Float.parseFloat(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0F;
        }
    }
    public static double toDouble(Object obj) {
        if (isBlank(obj)) {
            return 0D;
        }
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        try {
            return Double.parseDouble(obj.toString().trim());
        } catch (NumberFormatException e) {
            return 0D;
        }
    }
    public static boolean isNumber(Object obj) {
        if (isBlank(obj)) {
            return false;
        }
        if (obj instanceof Number) {
            return true;
        }
        try {
            Double.parseDouble(obj.toString().trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static boolean isNotNumber(Object obj) {
        return !isNumber(obj);
    }

    /** 将数值转换成 ipv4, 类似于 mysql 中 INET_NTOA(134744072) ==> 8.8.8.8 */
    public static String num2ip(long num) {
        return ((num & 0xff000000) >> 24) + "." + ((num & 0xff0000) >> 16)
                + "." + ((num & 0xff00) >> 8) + "." + ((num & 0xff));
    }
    /** 将 ipv4 的地址转换成数值. 类似于 mysql 中 INET_ATON('8.8.8.8') ==> 134744072 */
    public static long ip2num(String ip) {
        long result = 0;
        try {
            for (byte b : java.net.InetAddress.getByName(ip).getAddress()) {
                if ((b & 0x80L) != 0) {
                    result += 256L + b;
                } else {
                    result += b;
                }
                result <<= 8;
            }
            result >>= 8;
        } catch (java.net.UnknownHostException e) {
            assertException("「" + ip + "」不是有效的 ip 地址");
        }
        return result;
    }
    // ========== number ==========


    // ========== object & string ==========
    private static final Pattern THOUSANDS_REGEX = Pattern.compile("(\\d)(?=(?:\\d{3})+$)");
    /** 1234567890.51 ==> 1,234,567,890.51 */
    public static String formatNumberToThousands(String number) {
        if (isBlank(number)) {
            return EMPTY;
        }
        String left, right;
        if (number.contains(".")) {
            int point = number.indexOf(".");
            left = number.substring(0, point);
            right = number.substring(point);
        } else {
            left = number;
            right = EMPTY;
        }
        return THOUSANDS_REGEX.matcher(left).replaceAll("$1,") + right;
    }

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else if (obj1 != null && obj2 != null) {
            String s1 = obj1.toString();
            String s2 = obj2.toString();
            if (s1.length() != s2.length()) {
                return false;
            } else {
                return s1.equals(s2);
            }
        } else {
            return false;
        }
    }
    public static boolean equalsIgnoreCase(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return true;
        } else if (obj1 != null && obj2 != null) {
            String s1 = obj1.toString();
            String s2 = obj2.toString();
            if (s1.length() != s2.length()) {
                return false;
            } else {
                return s1.equalsIgnoreCase(s2);
            }
        } else {
            return false;
        }
    }

    public static String toStr(Object obj) {
        return isBlank(obj) ? EMPTY : obj.toString();
    }

    /** 如果字符长度大于指定长度, 则只输出头尾的固定字符 */
    public static String toStr(Object obj, int maxLen, int leftRightLen) {
        String str = toStr(obj);
        if (isBlank(str)) {
            return EMPTY;
        }

        int length = str.length();
        if (length > maxLen) {
            int returnLength = leftRightLen * 2 + 5;
            if (maxLen > returnLength) {
                return str.substring(0, leftRightLen) + " ... " + str.substring(length - leftRightLen, length);
            }
        }
        return str;
    }

    /** 如果字符小于指定的位数, 就在前面补全指定的字符 */
    public static String toStr(Object obj, int minLen, String completion) {
        String str = toStr(obj);
        if (isBlank(str)) {
            return EMPTY;
        }

        int length = str.length();
        if (length >= minLen) {
            return str;
        }

        StringBuilder sbd = new StringBuilder();
        int loop = minLen - length;
        for (int i = 0; i < loop; i++) {
            sbd.append(completion);
        }
        return sbd.append(str).toString();
    }

    /** 去掉所有的制表符 和 换行符 */
    public static String replaceTabAndWrap(String str) {
        return isBlank(str) ? EMPTY : str.replace("\t", EMPTY).replace("\n", EMPTY);
    }
    /** 将字符串中的多个空白符替换成一个 */
    public static String replaceBlank(String str) {
        return isBlank(str)
                ? EMPTY
                : MULTI_SPACE_REGEX.matcher(str.replace("\r", EMPTY).replace("\n", BLANK)).replaceAll(BLANK).trim();
    }

    /** 对象为 null, 或者其字符串形态为 空白符, "null", "undefined" 时返回 true */
    public static boolean isBlank(Object obj) {
        if (obj == null) {
            return true;
        }

        String str = obj.toString().trim();
        if (EMPTY.equals(str)) {
            return true;
        }

        String lower = str.toLowerCase();
        return "null".equals(lower) || "undefined".equals(lower);
    }
    /** 对象非空时返回 true */
    public static boolean isNotBlank(Object obj) {
        return !isBlank(obj);
    }

    /** 对象长度在指定的数值以内(不包含边距)就返回 true */
    public static boolean length(Object obj, int max) {
        return obj != null && obj.toString().trim().length() < max;
    }

    /** 对象长度在指定的数值经内(包含边距)就返回 true */
    public static boolean lengthBorder(String str, int min, int max) {
        return !isBlank(str) && str.length() >= min && str.length() <= max;
    }
    /** 对象长度在指定的数值以内(不包含边距)就返回 true */
    public static boolean length(String str, int min, int max) {
        return lengthBorder(str, min + 1, max - 1);
    }

    private static final Pattern ALL = Pattern.compile(".");
    /** 将字符串中指定位数的值模糊成 * 并返回. 索引位从 0 开始 */
    public static String foggy(String param, int start, int end) {
        if (isBlank(param)) {
            return EMPTY;
        }
        if (start < 0 || end < start || end > param.length()) {
            return param;
        }

        return param.substring(0, start) + ALL.matcher(param.substring(start, end)).replaceAll("*") + param.substring(end);
    }

    public static String like(String param) {
        return isBlank(param) ? EMPTY : LIKE + param + LIKE;
    }
    public static String leftLike(String param) {
        return isBlank(param) ? EMPTY : LIKE + param;
    }
    public static String rightLike(String param) {
        return isBlank(param) ? EMPTY : param + LIKE;
    }
    // ========== object & string ==========


    // ========== regex ==========
    /**
     * 验证 指定正则 是否 <span style="color:red;">全字匹配</span> 指定字符串, 匹配则返回 true <br/><br/>
     *
     * 左右空白符 : (?m)(^\s*|\s*$)<br>
     * 空白符 : (^\\s*)|(\\s*$)<br/>
     * 匹配多行注释 : /\*\*(\s|.)*?\* /<br/>
     */
    public static boolean checkRegexWithStrict(String param, String regex) {
        return isNotBlank(param) && Pattern.compile(regex).matcher(param).matches();
    }
    /** 后缀是图片则返回 true */
    public static boolean checkImage(String image) {
        return checkRegexWithStrict(image, IMAGE);
    }
    /** 是正确的邮箱地址则返回 true */
    public static boolean checkEmail(String email) {
        return checkRegexWithStrict(email, EMAIL);
    }
    /** 是一个手机则返回 true. 可以使用 - 和空格. 131-1234-5678, 131 1234 5678 是正确的手机号, 131-1234 5678 不是 */
    public static boolean checkPhone(String phone) {
        return checkRegexWithStrict(phone, PHONE);
    }
    /** 是一个有效的 ip 地址则返回 true */
    public static boolean isLicitIp(String ip) {
        return checkRegexWithStrict(ip, IPV4);
    }
    /** 是一个有效的身份证号就返回 true */
    public static boolean isIdCard(String num) {
        return checkRegexWithStrict(num, ID_CARD);
    }
    /** 基于身份证号码返回性别 */
    public static String idCardToGender(String num) {
        if (isIdCard(num)) {
            int len = num.length();
            String g;
            if (len == 15) {
                g = num.substring(14, 15);
            } else if (len == 18) {
                g = num.substring(16, 17);
            } else {
                g = EMPTY;
            }
            return isNumber(g) ? (toInt(g) % 2 == 0 ? "女" : "男") : "未知";
        } else {
            return "未知";
        }
    }
    /** 是本地请求则返回 true */
    public static boolean isLocalRequest(String ip) {
        return checkRegexWithStrict(ip, LOCAL);
    }

    /** 只要找到匹配即返回 true */
    public static boolean checkRegexWithRelax(String param, String regex) {
        return isNotBlank(param) && Pattern.compile(regex).matcher(param).find();
    }

    /** 传入的参数只要包含字母就返回 true */
    public static boolean isLetter(String param) {
        return checkRegexWithRelax(param, LETTER);
    }
    /** 传入的参数只要包含中文就返回 true */
    public static boolean checkChinese(String param) {
        return checkRegexWithRelax(param, CHINESE);
    }
    /** 传入的参数只要来自移动端就返回 true */
    public static boolean checkMobile(String param) {
        return checkRegexWithRelax(param, MOBILE);
    }
    /** 传入的参数只要是 iOS 端就返回 true */
    public static boolean checkiOS(String param) {
        return checkRegexWithRelax(param, IOS);
    }
    /** 传入的参数只要是 android 端就返回 true */
    public static boolean checkAndroid(String param) {
        return checkRegexWithRelax(param, ANDROID);
    }
    /** 传入的参数只要是 pc 端就返回 true */
    public static boolean checkPc(String param) {
        return checkRegexWithRelax(param, PC);
    }


    /** 将两个 int 合并成 long */
    public static long merge(int property, int value) {
        return ((long) property << 32) + (long) value;
    }
    /** 将 long 拆分两个 int */
    public static int[] parse(long pv) {
        return new int[] { (int) (pv >> 32), (int) pv };
    }


    /** 字符转义. 主要针对 url 传递给后台前的操作. 如 ? 转换为 %3F, = 转换为 %3D, & 转换为 %26 等 */
    public static String urlEncode(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        try {
            // java 中的 encode 是把空格变成 +, 转义后需要将 + 替换成 %2B
            return URLEncoder.encode(src, StandardCharsets.UTF_8.displayName());//.replaceAll("\\+", "%2B");
        } catch (Exception e) {
            return src;
        }
    }
    /** 字符反转义, 主要针对 url 传递到后台后的操作 */
    public static String urlDecode(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        try {
            // java 中的 encode 是把空格变成 +, 反转义前需要将 %2B 替换成 +
            return URLDecoder.decode(src/*.replaceAll("%2B", "\\+")*/, StandardCharsets.UTF_8.displayName());
        } catch (Exception e) {
            return src;
        }
    }
    /** 给参数中的值转义 */
    public static String urlEncodeValue(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }

        if (!src.contains("=")) {
            return src;
        }
        StringBuilder sbd = new StringBuilder();
        String[] sp = src.split("&");
        for (int i = 0; i < sp.length; i++) {
            String[] split = sp[i].split("=");

            if (i > 0) {
                sbd.append("&");
            }
            sbd.append(split[0]).append("=");
            if (split.length == 2) {
                sbd.append(urlEncode(split[1]));
            }
        }
        return sbd.toString();
    }

    /** 生成不带 - 的 uuid */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", EMPTY);
    }

    /** 获取后缀(包含点 .) */
    public static String getSuffix(String file) {
        return (isNotBlank(file) && file.contains("."))
                ? file.substring(file.lastIndexOf(".")) : EMPTY;
    }

    /** 将传入的文件重命名成不带 - 的 uuid 名称并返回 */
    public static String renameFile(String fileName) {
        return uuid() + getSuffix(fileName);
    }

    /** 拼接 url 和 path */
    public static String appendUrlAndPath(String url, String path) {
        return addSuffix(url) + addPrefix(path).substring(1);
    }
    /** 为空则返回空字符串, 如果传入的 url 中有 ? 则在尾部拼接 &, 否则拼接 ? 返回 */
    public static String appendUrl(String src) {
        if (isBlank(src)) {
            return EMPTY;
        }
        return src + (src.contains("?") ? "&" : "?");
    }
    /** 为空则返回 /, 如果开头有 / 则直接返回, 否则在开头拼接 / 并返回 */
    public static String addPrefix(String src) {
        if (isBlank(src)) {
            return "/";
        }
        if (src.startsWith("/")) {
            return src;
        }
        return "/" + src;
    }
    /** 为空则返回 /, 如果结尾有 / 则直接返回, 否则在结尾拼接 / 并返回 */
    public static String addSuffix(String src) {
        if (isBlank(src)) {
            return "/";
        }
        if (src.endsWith("/")) {
            return src;
        }
        return src + "/";
    }
    /** 从 url 中获取最后一个 斜杆(/) 后的内容 */
    public static String getFileNameInUrl(String url) {
        if (isBlank(url) || !url.contains("/")) {
            return EMPTY;
        }
        // 只截取到 ? 处, 如果有的话
        int last = url.contains("?") ? url.lastIndexOf("?") : url.length();
        return url.substring(url.lastIndexOf("/") + 1, last);
    }

    /** 当值为 null, 空白符, "null" 时, 返回空字符串 */
    public static String getNil(Object obj) {
        return isBlank(obj) ? EMPTY : obj.toString().trim();
    }

    /** 属性转换成方法, 加上 get 并首字母大写 */
    public static String fieldToMethod(String field) {
        if (isBlank(field)) {
            return EMPTY;
        }
        field = field.trim();
        if (isBlank(field)) {
            return EMPTY;
        }
        return  "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    /**
     * 调用对象的属性对应的 get 方法
     *
     * @param data  对象
     * @param field 属性名
     * @return 如果属性是枚举则调用枚举的 getValue 方法, 如果是日期则格式化, 否则返回此属性值的 toString 方法
     */
    @SuppressWarnings("rawtypes")
    public static String getField(Object data, String field) {
        if (isBlank(data) || isBlank(field)) {
            return EMPTY;
        }

        String[] split = field.split("\\|");
        Object value;
        if (data instanceof Map) {
            value = ((Map) data).get(split[0]);
        } else {
            value = getMethod(data, fieldToMethod(split[0]));
        }

        if (isBlank(value)) {
            Class<?> fieldType = getFieldType(data, field);
            return (isNotBlank(fieldType) && fieldType == Money.class) ? "0" : EMPTY;
        } else if (value.getClass().isEnum()) {
            // 如果是枚举, 则调用其 getValue 方法, getValue 没有值则使用枚举的 name
            Object enumValue = getMethod(value, "getValue");
            return getNil(enumValue != null ? enumValue : value);
        } else if (value instanceof Date) {
            // 如果是日期, 则格式化
            if (split.length > 1 && isNotBlank(split[1])) {
                return getNil(DateUtil.format((Date) value, split[1]));
            } else {
                return getNil(DateUtil.formatDateTime((Date) value));
            }
        } else {
            return getNil(value);
        }
    }

    /** 调用对象的公有方法. 异常将被忽略并返回 null */
    public static Object getMethod(Object obj, String method, Object... param) {
        if (isNotBlank(method)) {
            try {
                return obj.getClass().getDeclaredMethod(method).invoke(obj, param);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
            }
            // getMethod 会将从父类继承过来的 public 方法也查询出来
            try {
                return obj.getClass().getMethod(method).invoke(obj, param);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignore) {
            }
        }
        return null;
    }

    public static Class<?> getFieldType(Object obj, String field) {
        if (isNotBlank(field)) {
            try {
                Field f = obj.getClass().getDeclaredField(field);
                if (isNotBlank(f)) {
                    return f.getType();
                }
            } catch (NoSuchFieldException ignore) {
            }
            // getMethod 会将从父类继承过来的 public 方法也查询出来
            try {
                Field f = obj.getClass().getField(field);
                if (isNotBlank(f)) {
                    return f.getType();
                }
            } catch (NoSuchFieldException ignore) {
            }
        }
        return null;
    }

    /** 转换成 id=123&name=xyz&name=opq */
    public static String formatParam(Map<String, ?> params) {
        if (A.isEmpty(params)) {
            return EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (isNotBlank(key) && isNotBlank(value)) {
                if (i > 0) {
                    sbd.append("&");
                }
                String v = "password".equalsIgnoreCase(key) ? "***" : A.toString(value);
                sbd.append(key).append("=").append(v);
                i++;
            }
        }
        return sbd.toString();
    }

    /** 获取指定类所在 jar 包的地址 */
    public static String getClassInFile(Class<?> clazz) {
        if (isNotBlank(clazz)) {
            ProtectionDomain domain = clazz.getProtectionDomain();
            if (isNotBlank(domain)) {
                CodeSource source = domain.getCodeSource();
                if (isNotBlank(source)) {
                    URL location = source.getLocation();
                    if (isNotBlank(location)) {
                        return location.getFile();
                    }
                }
            }
        }
        return null;
    }


    /** 对象为 null、空白符、"null" 字符串时, 则抛出异常 */
    public static void assertNil(Object obj, String msg) {
        if (isBlank(obj)) {
            assertException(msg);
        }
    }

    /** 数组为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(T[] array, String msg) {
        if (A.isEmpty(array)) {
            assertException(msg);
        }
    }

    /** 列表为 null 或 长度为 0 时则抛出异常 */
    public static <T> void assertEmpty(Collection<T> list, String msg) {
        if (A.isEmpty(list)) {
            assertException(msg);
        }
    }

    /** map 为 null 或 长度为 0 时则抛出异常 */
    public static <K,V> void assertEmpty(Map<K,V> map, String msg) {
        if (A.isEmpty(map)) {
            assertException(msg);
        }
    }

    /** 数值为空或小于等于 0 则抛出异常 */
    public static void assert0(Number number, String msg) {
        if (less0(number)) {
            assertException(msg);
        }
    }

    /** 条件为 true 则抛出业务异常 */
    public static void assertException(Boolean flag, String msg) {
        if (flag != null && flag) {
            assertException(msg);
        }
    }

    public static void assertException(String msg) {
        serviceException(msg);
    }

    /** 错误的请求 */
    public static void badRequestException(String msg) {
        throw new BadRequestException(msg);
    }
    /** 需要权限 */
    public static void forbiddenException(String msg) {
        throw new ForbiddenException(msg);
    }
    /** 404 */
    public static void notFoundException(String msg) {
        throw new NotFoundException(msg);
    }
    /** 未登录 */
    public static void notLoginException() {
        throw new NotLoginException();
    }
    /** 业务异常 */
    public static void serviceException(String msg) {
        throw new ServiceException(msg);
    }

    /** 条件为 true 则抛出必须处理的异常 */
    public static void assertMustHandleException(Boolean flag, String msg) throws ServiceMustHandleException {
        if (flag != null && flag) {
            throw new ServiceMustHandleException(msg);
        }
    }

    public static String returnMsg(Throwable e, boolean online) {
        String msg;
        if (online) {
            msg = "请求时出现" + (e instanceof NullPointerException ? "问题" : "错误");
        } else if (e instanceof NullPointerException) {
            msg = "空指针异常, 联系后台查看日志进行处理";
        } else {
            msg = e.getMessage();
        }
        return msg;
    }
}
