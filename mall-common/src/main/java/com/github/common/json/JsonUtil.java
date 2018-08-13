package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.util.LogUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public class JsonUtil {

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
            // 提升序列化性能, map 中的 null 值不序列化
            // configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false);
            // 提升序列化性能, null 值不序列化
            // setSerializationInclusion(JsonInclude.Include.NON_NULL);
            // 允许字符串中包含 未加引号的控制字符(值小于 32 的 ASCII 字符, 包括制表符和换行字符)的功能.
            // 如果功能设置为 false, 则遇到此类字符时会引发异常. 由于 json 规范要求引用所有控制字符, 因此这是非标准功能, 默认情况下是禁用的.
            configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

            // 如果用 BigDecimal 表示金额时的处理: 保留两位小数的精度返回
            registerModule(new SimpleModule().addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
                @Override
                public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider ser) throws IOException {
                    if (value == null) {
                        value = BigDecimal.ZERO;
                    }
                    gen.writeObject(value.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString());
                }
            }));
        }
    }

    /** 对象转换, 失败将会返回 null */
    public static <S,T> T convert(S source, Class<T> clazz) {
        return toObjectNil(toJson(source), clazz);
    }
    /** 集合转换, 失败将会返回 null */
    public static <S,T> List<T> convertList(List<S> sourceList, Class<T> clazz) {
        return toListNil(toJson(sourceList), clazz);
    }

    public static <T,S> T convert(S source, TypeReference<?> type) {
        return toObjectNil(toJson(source), type);
    }

    /** 对象转换成 json 字符串 */
    public static String toJson(Object obj) {
        try {
            return RENDER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object(" + obj + ") to json exception.", e);
        }
    }

    /** 将 json 字符串转换为对象 */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return RENDER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Object(%s) exception", json, clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为对象, 当转换异常时, 返回 null */
    public static <T> T toObjectNil(String json, Class<T> clazz) {
        try {
            return RENDER.readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("json(%s) to Object(%s) exception", json, clazz.getName()), e);
            }
            return null;
        }
    }
    /** 将 json 字符串转换为泛型对象 */
    public static <T> T toObjectNil(String json, TypeReference<?> type) {
        try {
            return RENDER.readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("json(%s) to Object(%s) exception", json, type.getClass().getName()), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to List<%s> exception", json, clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toListNil(String json, Class<T> clazz) {
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to List<%s> exception", json, clazz.getName()), e);
            }
            return null;
        }
    }
}
