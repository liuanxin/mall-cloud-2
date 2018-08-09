package com.github.common.resource;

import com.github.common.util.A;
import com.github.common.util.LogUtil;
import com.github.common.util.U;
import com.google.common.collect.Lists;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 收集资源文件(这里主要是指 mybatis 的 xml 文件)的工具类 */
public final class CollectResourceUtil {

    /**
     * 获取多个包底下 mybatis 要加载的 xml 文件.
     *
     * key 表示资源文件所在包的类(用来获取 ClassLoader), value 表示目录路径(数组)
     */
    public static Resource[] resource(Map<Class, String[]> resourceMap) {
        List<Resource> resourceList = Lists.newArrayList();
        for (Map.Entry<Class, String[]> entry : resourceMap.entrySet()) {
            // 将模块里面的 mybatis 配置文件都收集起来扫描进 spring 容器
            resourceList.addAll(getResourceArray(entry.getKey(), entry.getValue()));
        }
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("mybatis load xml:({})", A.toStr(resourceList));
        }
        return resourceList.toArray(new Resource[resourceList.size()]);
    }

    /** 从单个类所在的加载器下获取 mybatis 要加载的 xml 文件 */
    private static List<Resource> getResourceArray(Class clazz, String[] resourcePath) {
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("load {} in ({})", clazz, U.getClassInFile(clazz));
        }
        List<Resource> resourceList = Lists.newArrayList();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(clazz.getClassLoader());
        for (String path : resourcePath) {
            try {
                Resource[] resources = resolver.getResources(path);
                if (A.isNotEmpty(resources)) {
                    Collections.addAll(resourceList, resources);
                }
            } catch (IOException e) {
                if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                    LogUtil.ROOT_LOG.error(String.format("load file(%s) exception: %s", path, e.getMessage()));
                }
            }
        }
        return resourceList;
    }
}
