package com.github.product.config;

import com.github.common.resource.CollectTypeHandlerUtil;
import com.github.common.resource.CollectResourceUtil;
import com.github.common.util.A;
import com.github.global.constant.GlobalConst;
import com.github.product.constant.ProductConst;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.core.io.Resource;

/**
 * 商品模块的配置数据. 主要是 mybatis 的多配置目录和类型处理器
 *
 * @author https://github.com/liuanxin
 */
final class ProductConfigData {

    private static final String[] RESOURCE_PATH = new String[] {
            ProductConst.MODULE_NAME + "/*.xml",
            ProductConst.MODULE_NAME + "-custom/*.xml"
    };
    /** 要加载的 mybatis 的配置文件目录 */
    static final Resource[] RESOURCE_ARRAY = CollectResourceUtil.resource(A.maps(
            ProductConfigData.class, RESOURCE_PATH
    ));
    
    /** 要加载的 mybatis 类型处理器的目录 */
    static final TypeHandler[] HANDLER_ARRAY = CollectTypeHandlerUtil.typeHandler(A.maps(
            GlobalConst.MODULE_NAME, GlobalConst.class,
            ProductConst.MODULE_NAME, ProductConfigData.class
    ));
}
