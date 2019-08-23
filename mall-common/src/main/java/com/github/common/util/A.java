package com.github.common.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.lang.reflect.Array;
import java.util.*;

/** 集合相关的工具包 */
public final class A {

    private static final String SPLIT = ",";

    public static boolean isArray(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Collection);
    }
    public static boolean isNotArray(Object obj) {
        return !isArray(obj);
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.size() == 0;
    }
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }

    public static String toStr(Collection<?> collection) {
        return toStr(collection, SPLIT);
    }
    public static String toStr(Collection<?> collection, String split) {
        StringBuilder sbd = new StringBuilder();
        int i = 0;
        for (Object obj : collection) {
            if (i > 0) {
                sbd.append(split);
            }
            sbd.append(obj);
            i++;
        }
        return sbd.toString();
    }
    public static String toStringWithArrayOrCollection(Object arrayOrCollection) {
        return toStringWithArrayOrCollection(arrayOrCollection, SPLIT);
    }
    public static String toStringWithArrayOrCollection(Object arrayOrCollection, String split) {
        if (arrayOrCollection != null) {
            if (arrayOrCollection.getClass().isArray()) {
                StringBuilder sbd = new StringBuilder();
                int len = Array.getLength(arrayOrCollection);
                for (int i = 0; i < len; i++) {
                    if (i > 0) {
                        sbd.append(split);
                    }
                    sbd.append(U.toStr(Array.get(arrayOrCollection, i)));
                }
                return sbd.toString();
            } else if (arrayOrCollection instanceof Collection) {
                return toStr((Collection<?>) arrayOrCollection, split);
            } else {
                return arrayOrCollection.toString();
            }
        } else {
            return U.EMPTY;
        }
    }

    public static String toStr(Object[] array) {
        return toStr(array, SPLIT);
    }
    public static String toStr(Object[] array, String split) {
        if (isEmpty(array)) {
            return U.EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sbd.append(split);
            }
            sbd.append(array[i]);
        }
        return sbd.toString();
    }

    /** 数组去重返回 */
    public static <T> Collection<T> removeDuplicate(T[] source) {
        return removeDuplicate(Lists.newArrayList(source));
    }
    /** 删除重复的项 */
    public static <T> Collection<T> removeDuplicate(Collection<T> array) {
        Set<T> set = new LinkedHashSet<>(array);
        array.clear();
        array.addAll(set);
        return array;
    }

    /** 构造 HashMap, 必须保证每两个参数的类型是一致的! 当参数是奇数时, 最后一个 key 将会被忽略 */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> maps(Object... keysAndValues) {
        return (HashMap<K, V>) maps(Maps.newHashMap(), keysAndValues);
    }
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> maps(Map<K, V> result, Object... keysAndValues) {
        if (isNotEmpty(keysAndValues)) {
            for (int i = 0; i < keysAndValues.length; i += 2) {
                if (keysAndValues.length > (i + 1)) {
                    result.put((K) keysAndValues[i], (V) keysAndValues[i + 1]);
                }
            }
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    /** 构造 LinkedHashMap, 必须保证每两个参数的类型是一致的! 当参数是奇数时, 最后一个 key 将会被忽略 */
    public static <K, V> LinkedHashMap<K, V> linkedMaps(Object... keysAndValues) {
        return (LinkedHashMap<K, V>) maps(Maps.newLinkedHashMap(), keysAndValues);
    }

    /** 获取数组的第一个元素 */
    public static <T> T first(T[] array) {
        return isEmpty(array) ? null : array[0];
    }
    /** 获取集合的第一个元素 */
    public static <T> T first(Collection<T> collection) {
        return isEmpty(collection) ? null : collection.iterator().next();
    }
    /** 获取集合的最后一个元素 */
    public static <T> T last(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }

        // 当类型为 List 时, 直接取得最后一个元素
        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            return list.get(list.size() - 1);
        }
        // 其他类型通过 iterator 滚动到最后一个元素
        Iterator<T> iterator = collection.iterator();
        while (true) {
            T current = iterator.next();
            if (!iterator.hasNext()) {
                return current;
            }
        }
    }

    /** 集合中随机返回一个 */
    @SuppressWarnings("unchecked")
    public static <T> T rand(Collection<T> source) {
        return isEmpty(source) ? null : (T) source.toArray()[U.RANDOM.nextInt(source.size())];
    }
}
