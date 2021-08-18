package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.common.date.DateFormatType;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class JsonUtil {

    /*
    使用 JacksonXml 可以像 json 一样使用相关的 api
    ObjectMapper RENDER = new XmlMapper();

    // object to xml
    String xml = RENDER.writeValueAsString(request);

    // xml to object
    Parent<Child> parent = RENDER.readValue(xml, new TypeReference<Parent<Child>>() {});

    但是需要引入一个包
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
        <version>...</version>
    </dependency>
    */

    public static final ObjectMapper RENDER = new RenderObjectMapper();
    public static final ObjectMapper IGNORE_PROPERTY_RENDER = new RenderObjectMapper();
    static {
        IGNORE_PROPERTY_RENDER.configure(MapperFeature.USE_ANNOTATIONS, false);
    }

    private static class RenderObjectMapper extends ObjectMapper {
        private static final long serialVersionUID = 0L;
        private RenderObjectMapper() {
            super();
            // NON_NULL: null 值不序列化, NON_EMPTY: null 空字符串、长度为 0 的 list、map 都不序列化
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            // 时间格式. 要想自定义在字段上标 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 即可
            SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormatType.YYYY_MM_DD_HH_MM_SS.getValue());
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            setDateFormat(dateFormat);

            // 不确定值的枚举返回 null
            enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
            // 允许字符串中包含未加引号的控制字符(值小于 32 的 ASCII 字符, 包括制表符和换行字符)
            // json 标准要求所有控制符必须使用引号, 因此默认是 false, 遇到此类字符时会抛出异常
            // enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);

            // 日期不用 utc 方式显示(utc 是一个整数值)
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // 不确定的属性项上不要失败
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

            registerModule(JsonModule.dateDeserializer());
            registerModule(JsonModule.bigDecimalSerializer());
        }
    }

    /** 对象转换, 失败将会返回 null */
    public static <S,T> T convertBasic(S source, Class<T> clazz) {
        return U.isNull(source) ? null : toObjectNil(toJsonNil(source), clazz);
    }
    /** 集合转换, 失败将会返回 null */
    public static <S,T> List<T> convertListBasic(Collection<S> sourceList, Class<T> clazz) {
        return A.isEmpty(sourceList) ? Collections.emptyList() : toListNil(toJsonNil(sourceList), clazz);
    }

    /** 对象转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <S,T> T convert(S source, Class<T> clazz) {
        if (U.isNull(source)) {
            return null;
        }

        String json;
        if (source instanceof String) {
            json = (String) source;
        } else {
            try {
                json = IGNORE_PROPERTY_RENDER.writeValueAsString(source);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error(String.format("Object(%s) to json exception", U.compress(source.toString())), e);
                }
                return null;
            }
        }

        if (U.isBlank(json)) {
            return null;
        }
        try {
            return IGNORE_PROPERTY_RENDER.readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to Object(%s) exception", U.compress(json), clazz.getName()), e);
            }
            return null;
        }
    }
    /** 集合转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <S,T> List<T> convertList(Collection<S> sourceList, Class<T> clazz) {
        if (A.isEmpty(sourceList)) {
            return Collections.emptyList();
        }

        String json;
        try {
            json = IGNORE_PROPERTY_RENDER.writeValueAsString(sourceList);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("List(%s) to json exception", U.compress(sourceList.toString())), e);
            }
            return Collections.emptyList();
        }

        if (U.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return IGNORE_PROPERTY_RENDER.readValue(json, IGNORE_PROPERTY_RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("List(%s) to List<%s> exception", U.compress(json), clazz.getName()), e);
            }
            return Collections.emptyList();
        }
    }

    public static <T,S> T convertType(S source, TypeReference<T> type) {
        if (U.isNull(source)) {
            return null;
        }
        return toObjectNil((source instanceof String) ? ((String) source) : toJsonNil(source), type);
    }

    /** 对象转换成 json 字符串 */
    public static String toJson(Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        try {
            return RENDER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("object(" + U.compress(obj.toString()) + ") to json exception.", e);
        }
    }
    /** 对象转换成 json 字符串 */
    public static String toJsonNil(Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        try {
            return RENDER.writeValueAsString(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Object(" + U.compress(obj.toString()) + ") to json exception", e);
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
            throw new RuntimeException(String.format("json(%s) to Object(%s) exception", U.compress(json), clazz.getName()), e);
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
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to Object(%s) exception", U.compress(json), clazz.getName()), e);
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
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to Object(%s) exception", U.compress(json), type.getClass().getName()), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to List<%s> exception", U.compress(json), clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为指定的数组列表 */
    public static <T> List<T> toListNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return RENDER.readValue(json, RENDER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error(String.format("json(%s) to List<%s> exception", U.compress(json), clazz.getName()), e);
            }
            return Collections.emptyList();
        }
    }
}
