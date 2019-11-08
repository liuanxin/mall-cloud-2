package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

public class JsonUtil {

    private static final int MAX_LEN = 500;
    private static final int LEFT_RIGHT_LEN = 100;

    public static final ObjectMapper RENDER = new RenderObjectMapper();

    private static class RenderObjectMapper extends ObjectMapper {
        private static final long serialVersionUID = 0L;
        private RenderObjectMapper() {
            super();
            // 日期不用 utc 方式显示(utc 是一个整数值)
            // configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            // 时间格式. 要想自定义在字段上标 @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") 即可
            // setDateFormat(new SimpleDateFormat(DateFormatType.YYYY_MM_DD_HH_MM_SS.getValue()));
            // 不确定值的枚举返回 null
            configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
            // 不确定的属性项上不要失败
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // 允许字符串中包含未加引号的控制字符(值小于 32 的 ASCII 字符, 包括制表符和换行字符)
            // json 标准要求所有控制符必须使用引号, 因此默认是 false, 遇到此类字符时会抛出异常
            configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            // NON_NULL: null 值不序列化, NON_EMPTY: null 空字符串、长度为 0 的 list、map 都不序列化
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            // 如果用 BigDecimal 表示金额时的处理: 保留两位小数的精度返回
            registerModule(new SimpleModule().addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public void serialize(BigDecimal money, JsonGenerator gen, SerializerProvider ser) throws IOException {
                    if (U.isNotBlank(money)) {
                        gen.writeObject(money.setScale(2, RoundingMode.DOWN));
                    }
                }
            }));
        }
    }

    /** 对象转换, 失败将会返回 null */
    public static <S,T> T convert(S source, Class<T> clazz) {
        return U.isBlank(source) ? null : toObjectNil(toJson(source), clazz);
    }
    /** 集合转换, 失败将会返回 null */
    public static <S,T> List<T> convertList(List<S> sourceList, Class<T> clazz) {
        return A.isEmpty(sourceList) ? Collections.emptyList() : toListNil(toJson(sourceList), clazz);
    }

    public static <T,S> T convert(S source, TypeReference<T> type) {
        return U.isBlank(source) ? null : toObjectNil(toJson(source), type);
    }

    /** 对象转换成 json 字符串 */
    public static String toJson(Object obj) {
        if (U.isBlank(obj)) {
            return null;
        }
        try {
            return RENDER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object(" + U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN) + ") to json exception.", e);
        }
    }
    /** 对象转换成 json 字符串 */
    public static String toJsonNil(Object obj) {
        if (U.isBlank(obj)) {
            return null;
        }
        try {
            return RENDER.writeValueAsString(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info("Object(" + U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN) + ") to json exception", e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为对象 */
    public static <T> T toObject(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return RENDER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Object(%s) exception",
                    U.toStr(json, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为对象, 当转换异常时, 返回 null */
    public static <T> T toObjectNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return RENDER.readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("json(%s) to Object(%s) exception",
                        U.toStr(json, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
            }
            return null;
        }
    }
    /** 将 json 字符串转换为泛型对象 */
    public static <T> T toObjectNil(String json, TypeReference<T> type) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return RENDER.readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("json(%s) to Object(%s) exception",
                        U.toStr(json, MAX_LEN, LEFT_RIGHT_LEN), type.getClass().getName()), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to List<%s> exception",
                    U.toStr(json, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toListNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to List<%s> exception",
                        U.toStr(json, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
            }
            return null;
        }
    }
}
