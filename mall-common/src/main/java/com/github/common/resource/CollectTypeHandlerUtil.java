package com.github.common.resource;

import com.github.common.Const;
import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Lists;
import org.apache.ibatis.type.TypeHandler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** 收集 mybatis 的类型处理工具类 */
@SuppressWarnings("rawtypes")
public final class CollectTypeHandlerUtil {

    /**
     * 获取多个包底下 mybatis 要加载的 类型处理类
     *
     * key 表示模块名(包含在类型处理所在类的包名上), value 表示枚举类所在包的类(用来获取 ClassLoader)
     */
    public static TypeHandler[] typeHandler(Map<String, Class> moduleMap) {
        List<TypeHandler> handlerList = Lists.newArrayList();
        for (Map.Entry<String, Class> entry : moduleMap.entrySet()) {
            // 将模块里面的 mybatis 类型处理器都收集起来装载进 mybatis 上下文
            handlerList.addAll(getHandleArray(entry.getValue(), Const.handlerPath(entry.getKey())));
        }
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("mybatis load type handle:({})", A.toStr(handlerList));
        }
        return handlerList.toArray(new TypeHandler[0]);
    }

    /** 基于指定的类(用来获取 ClassLoader), 在指定的包名下获取 mybatis 的类型处理器 */
    private static List<TypeHandler> getHandleArray(Class clazz, String classPackage) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("{} in ({})", clazz, U.getClassInFile(clazz));
        }
        List<TypeHandler> handlerList = Lists.newArrayList();
        String packageName = classPackage.replace(".", "/");
        URL url = clazz.getClassLoader().getResource(packageName);
        if (url != null) {
            if ("file".equals(url.getProtocol())) {
                File parent = new File(url.getPath());
                if (parent.isDirectory()) {
                    File[] files = parent.listFiles();
                    if (A.isNotEmpty(files)) {
                        for (File file : files) {
                            TypeHandler handler = getTypeHandler(file.getName(), classPackage);
                            if (handler != null) {
                                handlerList.add(handler);
                            }
                        }
                    }
                }
            } else if ("jar".equals(url.getProtocol())) {
                try (JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile()) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (name.startsWith(packageName) && name.endsWith(".class")) {
                            TypeHandler handler = getTypeHandler(name.substring(name.lastIndexOf("/") + 1), classPackage);
                            if (handler != null) {
                                handlerList.add(handler);
                            }
                        }
                    }
                } catch (IOException e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("can't load jar file", e);
                    }
                }
            }
        }
        return handlerList;
    }
    private static TypeHandler getTypeHandler(String name, String classPackage) {
        if (U.isNotBlank(name)) {
            String className = classPackage + "." + name.replace(".class", "");
            try {
                Class<?> clazz = Class.forName(className);
                if (TypeHandler.class.isAssignableFrom(clazz)) {
                    return (TypeHandler) clazz.getDeclaredConstructor().newInstance();
                }
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error(String.format("TypeHandler clazz (%s) exception: ", className), e);
                }
            }
        }
        return null;
    }
}
