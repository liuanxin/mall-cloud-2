package com.github.user.config;

import com.github.common.Const;
import com.github.liuanxin.page.PageInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 扫描指定目录. MapperScan 的处理类是 MapperScannerRegistrar, 其基于 ClassPathMapperScanner<br>
 *
 * @see org.mybatis.spring.annotation.MapperScannerRegistrar#registerBeanDefinitions
 * @see org.mybatis.spring.mapper.MapperScannerConfigurer#postProcessBeanDefinitionRegistry
 * @see org.mybatis.spring.mapper.ClassPathMapperScanner
 */
@Configuration
@MapperScan(basePackages = Const.BASE_PACKAGE)
public class UserDataSourceInit {

    private final DataSource dataSource;

    public UserDataSourceInit(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        // 装载 xml 实现
        sessionFactory.setMapperLocations(UserConfigData.RESOURCE_ARRAY);
        // 装载 typeHandler 实现
        sessionFactory.setTypeHandlers(UserConfigData.HANDLER_ARRAY);
        // mybatis 的分页插件
        sessionFactory.setPlugins(new Interceptor[] { new PageInterceptor("mysql") });
        return sessionFactory.getObject();
    }

    /** 要构建 or 语句, 参考: http://www.mybatis.org/generator/generatedobjects/exampleClassUsage.html */
    @Bean(name = "sqlSessionTemplate", destroyMethod = "clearCache")
    public SqlSessionTemplate sqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory());
    }

    /*
     * 事务控制, 默认已经装载了
     *
     * @see DataSourceTransactionManagerAutoConfiguration
     */
    /*
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
    */
}
