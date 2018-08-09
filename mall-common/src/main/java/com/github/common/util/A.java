package com.github.common.util;

import java.util.*;

/** 集合相关的工具包 */
public final class A {

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

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.size() == 0;
    }
    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }
    public static boolean isNotEmpty(Map map) {
        return !isEmpty(map);
    }

    public static <T> String toStr(Collection<T> collection) {
        return toStr(collection, ",");
    }
    public static <T> String toStr(Collection<T> collection, String split) {
        return toStr(collection.toArray(), split);
    }
    public static String toStr(Object[] array) {
        return toStr(array, ",");
    }
    public static String toStr(Object[] array, String split) {
        if (isEmpty(array)) {
            return U.EMPTY;
        }

        StringBuilder sbd = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sbd.append(array[i]);
            if (i + 1 != array.length) {
                sbd.append(split);
            }
        }
        return sbd.toString();
    }

    public static Long[] convert(String[] ids) {
        if (isEmpty(ids)) {
            return null;
        }

        List<Long> returnData = lists();
        for (String id : ids) {
            Long num = U.toLong(id);
            if (num > 0) {
                returnData.add(num);
            }
        }
        return returnData.toArray(new Long[returnData.size()]);
    }
    public static String[] convert(Long[] ids) {
        if (isEmpty(ids)) {
            return null;
        }

        List<String> returnData = lists();
        for (Long id : ids) {
            if (id != null && id > 0) {
                returnData.add(id.toString());
            }
        }
        return returnData.toArray(new String[returnData.size()]);
    }

    /** 字符串逗号分割后去重返回 */
    public static Collection<String> removeDuplicate(String source) {
        return removeDuplicate(source.split(",|，"));
    }
    /** 数组去重返回 */
    public static <T> Collection<T> removeDuplicate(T[] source) {
        return removeDuplicate(lists(source));
    }
    /** 删除重复的项 */
    public static <T> Collection<T> removeDuplicate(Collection<T> array) {
        Set<T> set = new LinkedHashSet<>(array);
        array.clear();
        array.addAll(set);
        return array;
    }

    /** 构造 ArrayList */
    @SuppressWarnings("unchecked")
    public static <T> List<T> lists(T... values) {
        return new ArrayList<T>(Arrays.asList(values));
    }
    /** 构造 LinkedList */
    @SuppressWarnings("unchecked")
    public static <T> List<T> linkedLists(T... values) {
        return new LinkedList<T>(Arrays.asList(values));
    }

    /** 构造 HashSet */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> sets(T... sets) {
        return new HashSet<T>(Arrays.asList(sets));
    }
    /** 构造 LinkedSet */
    @SuppressWarnings("unchecked")
    public static <T> Set<T> linkedSets(T... sets) {
        return new LinkedHashSet<T>(Arrays.asList(sets));
    }

    public static <K, V> HashMap<K, V> newHashMap() {
        return new HashMap<K, V>();
    }

    public static <K, V> HashMap<K, V> newLinkedHashMap() {
        return new LinkedHashMap<K, V>();
    }

    /** 构造 HashMap, 必须保证每两个参数的类型是一致的! 当参数是奇数时, 最后一个 key 将会被忽略 */
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> maps(Object... keysAndValues) {
        return (HashMap<K, V>) maps(newHashMap(), keysAndValues);
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
        return (LinkedHashMap<K, V>) maps(newLinkedHashMap(), keysAndValues);
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

    /**
     * 收集 List 对象中指定的方法(空参数且有返回值, 无返回值或调用异常将忽略), 生成一个新集合返回<br>
     * 此方法可以用 Lists.transform(list, Model::getProperty) 替代
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> selectMethod(Collection<?> from, String method) {
        if (isEmpty(from) || U.isBlank(method)) {
            return Collections.emptyList();
        }

        List result = linkedLists();
        for (Object obj : from) {
            Object value = U.getMethod(obj, U.fieldToMethod(method));
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
