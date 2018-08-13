package com.github.common;

import com.github.common.util.*;
import com.github.common.date.DateUtil;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.freemarker.FreeMarkerViewResolver;

import java.util.Map;

/** 项目里的视图渲染解析器, 这里主要是为了在上下文中注入一些公用类 */
public class RenderViewResolver extends FreeMarkerViewResolver {

    /** 静态资源用到的版本号 */
    private static String version = U.random(6);

    private static final BeansWrapper BEANS_WRAPPER = new BeansWrapperBuilder(Configuration.getVersion()).build();
    private static final TemplateHashModel STATIC_HASH_MODEL = BEANS_WRAPPER.getStaticModels();
    private static final TemplateHashModel ENUM_HASH_MODEL = BEANS_WRAPPER.getEnumModels();

    /** 一些全局的工具类 */
    private static final Class[] CLASSES = new Class[] {
            A.class, U.class, DateUtil.class, RequestUtils.class, Render.class
    };

    /** 构造器只加载一次 */
    public RenderViewResolver() {
        super();

        // Map<String, Object> context = Maps.newHashMap();
        // 使用下面这一句后, 页面上使用 ${C["...date.DateUtil"].now()}. 太长了! 使用下面的 putClass 来替代
        // context.put("C", STATIC_HASH_MODEL);
        // 使用下面这一句后, 页面上使用 ${E["...enums.OrderStatus"].Create} 获取枚举. 太长了! 使用下面的 putEnum 来替代
        // context.put("E", ENUM_HASH_MODEL);

        // 把工具类放入渲染的上下文中
        putClass(CLASSES);
    }
    /** 把「是否是线上环境 」放入渲染的全局上下文中. 只加载一次 */
    public RenderViewResolver putVariable(boolean online) {
        setAttributesMap(A.maps("online", online));
        return this;
    }
    /** 把类放入渲染的全局上下文中. 只加载一次 */
    public RenderViewResolver putClass(Class<?>... classes) {
        Map<String, Object> context = A.maps();
        if (A.isNotEmpty(classes)) {
            for (Class<?> clazz : classes) {
                String clazzName = clazz.getName();
                try {
                    context.put(clazz.getSimpleName(), STATIC_HASH_MODEL.get(clazzName));
                } catch (TemplateModelException e) {
                    if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                        LogUtil.ROOT_LOG.error("add class(" + clazzName + ") in Render context exception", e);
                    }
                }
            }
        }
        setAttributesMap(context);
        return this;
    }
    /**
     * <pre>
     * 把枚举放入渲染的上下文中. 只加载一次
     *
     * 假定要渲染的页面上下文中有一个 user 对象, 并且里面有 gender 这个枚举. GenderEnum 可以直接拿过来用:
     * &lt;#list GenderEnum?values as gender&gt;
     *   &lt;label>
     *     &lt;input type="radio" value="${gender.code}"&lt;#if user.gender == gender> checked="checked"&lt;/#if>>
     *     ${gender.getValue()}
     *   &lt;/label>
     * &lt;/#list&gt;
     * </pre>
     */
    public RenderViewResolver putEnum(Class<?>... enums) {
        Map<String, Object> context = A.maps();
        if (A.isNotEmpty(enums)) {
            for (Class<?> clazz : enums) {
                if (clazz.isEnum()) {
                    String clazzName = clazz.getName();
                    try {
                        context.put(clazz.getSimpleName() + "Enum", ENUM_HASH_MODEL.get(clazz.getName()));
                    } catch (TemplateModelException e) {
                        if (LogUtil.ROOT_LOG.isErrorEnabled()) {
                            LogUtil.ROOT_LOG.error("add enum(" + clazzName + ") in Render context exception", e);
                        }
                    }
                }
            }
        }
        setAttributesMap(context);
        return this;
    }

    @Override
    protected AbstractUrlBasedView buildView(String viewName) throws Exception {
        setAttributesMap(A.maps("version", version));
        return super.buildView(viewName);
    }

    public static String changeVersion() {
        version = U.random(6);
        return version;
    }
    public static String getVersion() {
        return version;
    }

}
