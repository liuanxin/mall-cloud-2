package com.github.common.resource;

import com.github.common.Const;
import com.github.common.util.U;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("rawtypes")
public final class CollectEnumUtil {

    /** 如果需要在 enum 中返回给前台的下拉数据, 添加一个这样的方法名, 返回结果用 Map. 如果有定义将无视下面的 code 和 value */
    private static final String METHOD = "select";
    /** 在 enum 中返回给前台的下拉数据的键, 如果没有将会以 ordinal() 为键 */
    private static final String CODE = "getCode";
    /** 在 enum 中返回给前台的下拉数据的值, 如果没有将会以 name() 为值 */
    private static final String VALUE = "getValue";

    /**
     * 从 指定模块指定类 获取所有的枚举类(放入 view 的上下文).
     *
     * key 表示模块名(包含在枚举所在类的包名上), value 表示枚举类所在包的类(用来获取 ClassLoader)
     */
    public static Class[] getEnumClass(Map<String, Class> enumMap) {
        Set<Class> set = Sets.newHashSet();
        for (Map.Entry<String, Class> entry : enumMap.entrySet()) {
            List<Class> enums = LoaderClass.getEnumArray(entry.getValue(), Const.enumPath(entry.getKey()));
            for (Class anEnum : enums) {
                if (U.isNotBlank(anEnum) && anEnum.isEnum()) {
                    // 将每个模块里面的枚举都收集起来, 然后会放入到渲染上下文里面去
                    set.add(anEnum);
                }
            }
        }
        return set.toArray(new Class[0]);
    }

    /** 获取所有枚举的说明 */
    @SuppressWarnings("rawtypes")
    public static Map<String, Map<String, Object>> enumMap(Map<String, Class> enumClassMap) {
        Map<String, Map<String, Object>> returnMap = Maps.newHashMap();
        for (Map.Entry<String, Class> entry : enumClassMap.entrySet()) {
            List<Class> enumList = LoaderClass.getEnumArray(entry.getValue(), Const.enumPath(entry.getKey()));
            for (Class anEnum : enumList) {
                if (U.isNotBlank(anEnum) && anEnum.isEnum()) {
                    returnMap.put(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, anEnum.getSimpleName()), enumInfo(anEnum));
                }
            }
        }
        return returnMap;
    }

    /** 根据枚举的名字获取单个枚举的说明. loadEnum 为 true 表示需要基于加载器去获取相关包里面的 枚举 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<String, Object> enumInfo(Class<?> enumClass) {
        try {
            // 在 enum 中如果有静态的 select 方法且返回的是 Map 就用这个
            Method select = enumClass.getMethod(METHOD);
            if (U.isNotBlank(select)) {
                Object result = select.invoke(null);
                if (U.isNotBlank(result) && result instanceof Map) {
                    return (Map<String, Object>) result;
                }
            }
        } catch (Exception ignore) {
        }

        Map<String, Object> returnMap = Maps.newHashMap();
        for (Object anEnum : enumClass.getEnumConstants()) {
            // 没有 getCode 方法就使用枚举的 ordinal
            Object key = U.getMethod(anEnum, CODE);
            if (key == null) {
                key = ((Enum) anEnum).ordinal();
            }

            // 没有 getValue 方法就使用枚举的 name
            Object value = U.getMethod(anEnum, VALUE);
            if (value == null) {
                value = ((Enum) anEnum).name();
            }

            returnMap.put(U.toStr(key), value);
        }
        return returnMap;
    }
}
