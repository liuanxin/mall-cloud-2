package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.github.common.date.DateFormatType;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
        <version>???</version>
    </dependency>
    */
    private static final Map<String, JavaType> TYPE_CACHE = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new RenderObjectMapper();

    private static final ObjectMapper EMPTY_OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectMapper IGNORE_OBJECT_MAPPER = new ObjectMapper();
    static {
        IGNORE_OBJECT_MAPPER.configure(MapperFeature.USE_ANNOTATIONS, false);
    }

    private static class RenderObjectMapper extends ObjectMapper {
        private RenderObjectMapper() {
            super();
            // NON_NULL  : null 值不序列化
            // NON_EMPTY : null、空字符串、长度为 0 的 list、长度为 0 的 map 都不序列化
            setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            globalConfig(this);
        }
    }

    public static void globalConfig(ObjectMapper objectMapper) {
        // 默认时间格式. 要自定义在字段上标 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 即可
        SimpleDateFormat dateFormat = new SimpleDateFormat(DateFormatType.YYYY_MM_DD_HH_MM_SS.getValue());
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        objectMapper.setDateFormat(dateFormat);

        // 日期不用 utc 方式显示(utc 是一个整数值)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 日期不用 utc 方式显示(utc 是一个整数值)
        objectMapper.disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
        // 不确定的属性项上不要失败
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 不确定值的枚举返回 null
        objectMapper.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
        // 用 BigDecimal 来反序列化浮点数
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        // 用 BigInteger 来反序列化整数
        objectMapper.enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);
        // 允许字符串中包含未加引号的控制字符(值小于 32 的 ASCII 字符, 包括制表符和换行字符)
        // json 标准要求所有控制符必须使用引号, 因此默认是 false, 遇到此类字符时会抛出异常
        // objectMapper.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature());
        objectMapper.registerModule(JsonModule.GLOBAL_MODULE);
    }


    /** 对象转换, 失败将会返回 null */
    public static <S,T> T convert(S source, Class<T> clazz) {
        return convert(source, clazz, false, false);
    }
    /** 集合转换, 失败将会返回空集合 */
    public static <S,T> List<T> convertList(Collection<S> sourceList, Class<T> clazz) {
        return convertList(sourceList, clazz, false, false);
    }
    /** 键值对转换, 失败将会返回空键值对 */
    public static <SK,SV,K,V> Map<K,V> convertMap(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass) {
        return convertMap(sourceMap, keyClass, valueClass, false, false);
    }
    /** 对象转换, 失败将会返回 null */
    public static <T,S> T convertType(S source, TypeReference<T> type) {
        return convertType(source, type, false, false);
    }

    /**
     * 对象转换, 失败将会返回 null
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @Json... 注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @Json... 注解
     */
    public static <S,T> T convert(S source, Class<T> clazz, boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        // noinspection DuplicatedCode
        if (U.isNull(source)) {
            return null;
        }

        String json;
        if (source instanceof String) {
            json = (String) source;
        } else {
            try {
                json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(source);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("obj({}) to json exception", U.compress(source.toString()), e);
                }
                return null;
            }
        }

        if (U.isBlank(json)) {
            return null;
        }
        try {
            return (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), clazz.getName(), e);
            }
            return null;
        }
    }
    /**
     * 集合转换, 失败将会返回空集合
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @JsonProperty 等注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @JsonProperty 等注解
     */
    public static <S,T> List<T> convertList(Collection<S> sourceList, Class<T> clazz,
                                            boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        if (A.isEmpty(sourceList)) {
            return Collections.emptyList();
        }

        String json;
        try {
            json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(sourceList);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("List({}) to json exception", U.compress(sourceList.toString()), e);
            }
            return Collections.emptyList();
        }
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            ObjectMapper mapper = (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER);
            return mapper.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> mapper.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyList();
        }
    }
    /**
     * 键值对转换, 失败将会返回空键值对
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @JsonProperty 等注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @JsonProperty 等注解
     */
    public static <SK,SV,K,V> Map<K,V> convertMap(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass,
                                                  boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        if (A.isEmpty(sourceMap)) {
            return Collections.emptyMap();
        }

        String json;
        try {
            json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(sourceMap);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Map({}) to json exception", U.compress(sourceMap.toString()), e);
            }
            return Collections.emptyMap();
        }
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            ObjectMapper mapper = (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER);
            return mapper.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> mapper.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyMap();
        }
    }

    /** 对象转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <S,T> T convertIgnoreAnnotation(S source, Class<T> clazz) {
        return convert(source, clazz, true, true);
    }
    /** 集合转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回空集合 */
    public static <S,T> List<T> convertListIgnoreAnnotation(Collection<S> sourceList, Class<T> clazz) {
        return convertList(sourceList, clazz, true, true);
    }
    /** 键值对转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回空键值对 */
    public static <SK,SV,K,V> Map<K,V> convertMapIgnoreAnnotation(Map<SK,SV> sourceMap, Class<K> keyClass, Class<V> valueClass) {
        return convertMap(sourceMap, keyClass, valueClass, true, true);
    }
    /** 对象转换(忽略 class 类属性上的 @Json... 注解), 失败将会返回 null */
    public static <T,S> T convertTypeIgnoreAnnotation(S source, TypeReference<T> type) {
        return convertType(source, type, true, true);
    }
    /**
     * 对象转换, 失败将会返回 null
     *
     * @param ignoreSourceAnnotation true 表示忽略 source 类属性上的 @Json... 注解
     * @param ignoreTargetAnnotation true 表示忽略 target 类属性上的 @Json... 注解
     */
    public static <S,T> T convertType(S source, TypeReference<T> type,
                                      boolean ignoreSourceAnnotation, boolean ignoreTargetAnnotation) {
        // noinspection DuplicatedCode
        if (U.isNull(source)) {
            return null;
        }

        String json;
        if (source instanceof String) {
            json = (String) source;
        } else {
            try {
                json = (ignoreSourceAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).writeValueAsString(source);
            } catch (Exception e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error("obj({}) to json exception", U.compress(source.toString()), e);
                }
                return null;
            }
        }

        if (U.isBlank(json)) {
            return null;
        }
        try {
            return (ignoreTargetAnnotation ? IGNORE_OBJECT_MAPPER : EMPTY_OBJECT_MAPPER).readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), type.getClass().getName(), e);
            }
            return null;
        }
    }

    /** 对象转换成 json 字符串 */
    public static String toJson(Object obj) {
        if (U.isNull(obj)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(String.format("object(%s) to json exception.", U.compress(obj.toString())), e);
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
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("Object({}) to json exception", U.compress(obj.toString()), e);
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
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Class(%s) exception", U.compress(json), clazz.getName()), e);
        }
    }
    /** 将 json 字符串转换为对象, 当转换异常时, 返回 null */
    public static <T> T toObjectNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), clazz.getName(), e);
            }
            return null;
        }
    }
    /** 将 json 字符串转换为泛型对象 */
    public static <T> T toObjectType(String json, TypeReference<T> type) {
        if (U.isBlank(json)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (IOException e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Class({}) exception", U.compress(json), type.getClass().getName(), e);
            }
            return null;
        }
    }

    /** 将 json 字符串转换为指定的集合, 转换失败则抛出 RuntimeException */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to List<%s> exception", U.compress(json), key), e);
        }
    }
    /** 将 json 字符串转换为指定的命令, 转换失败则返回空集合 */
    public static <T> List<T> toListNil(String json, Class<T> clazz) {
        if (U.isBlank(json)) {
            return Collections.emptyList();
        }

        String key = clazz.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(clazz.getName(),
                    fun -> OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to List<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyList();
        }
    }

    /** 将 json 字符串转换为指定的数组列表 */
    public static <K, V> Map<K, V> toMap(String json, final Class<K> keyClass, final Class<V> valueClass) {
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            throw new RuntimeException(String.format("json(%s) to Map<%s> exception", U.compress(json), key), e);
        }
    }
    /** 将 json 字符串转换为指定的数组列表 */
    public static <K, V> Map<K, V> toMapNil(String json, final Class<K> keyClass, final Class<V> valueClass) {
        if (U.isBlank(json)) {
            return Collections.emptyMap();
        }

        String key = keyClass.getName() + ", " + valueClass.getName();
        try {
            return OBJECT_MAPPER.readValue(json, TYPE_CACHE.computeIfAbsent(key,
                    fun -> OBJECT_MAPPER.getTypeFactory().constructMapType(LinkedHashMap.class, keyClass, valueClass)));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                LogUtil.ROOT_LOG.error("json({}) to Map<{}> exception", U.compress(json), key, e);
            }
            return Collections.emptyMap();
        }
    }
}
