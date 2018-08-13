package com.github.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.common.ModuleTest.*;

public class ModuleTest {

    /** true 生成断路器 */
    static boolean fallback = true;

    static final String PROJECT = "mall-cloud";
    static final String PACKAGE = "com.github";
    /** 注册中心的端口 */
    static String REGISTER_CENTER_PORT = "8761";
    private static final String PARENT = ModuleTest.class.getClassLoader().getResource("").getFile() + "../../../";
    static String PACKAGE_PATH = PACKAGE.replaceAll("\\.", "/");
    static String AUTHOR = " *\n * @author https://github.com/liuanxin\n";

    static String capitalize(String name) {
        StringBuilder sbd = new StringBuilder();
        for (String str : name.split("[.-]")) {
            sbd.append(str.substring(0, 1).toUpperCase()).append(str.substring(1));
        }
        return sbd.toString();
    }
    static void writeFile(File file, String content) {
        try (OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            write.write(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
//        generate("0-common",  "8070", "公共");
//        generate("0-queue",   "8071", "消息队列");
//        generate("0-search",  "8072", "搜索");
//        generate("1-user",    "8091", "用户");
//        generate("2-product", "8092", "商品");
//        generate("3-order",   "8093", "订单");

        soutInfo();
    }

    private static List<List<String>> moduleNameList = new ArrayList<>();
    private static List<List<String>> moduleList = new ArrayList<>();
    private static void generate(String basicModuleName, String port, String comment) throws Exception {
        String moduleName = "module-" + basicModuleName;
        String packageName = basicModuleName;
        if (basicModuleName.contains("-")) {
            packageName = basicModuleName.substring(basicModuleName.indexOf("-") + 1);
        }
        String model = packageName + "-model";
        String client = packageName + "-client";
        String server = packageName + "-server";
        String module = PARENT + moduleName;

        Parent.generateParent(moduleName, model, client, server, module, comment);
        Client.generateClient(moduleName, packageName, client, module, comment);
        Model.generateModel(moduleName, packageName, model, module, comment);
        Server.generateServer(moduleName, packageName, model, server, module, port, comment);

        moduleNameList.add(Arrays.asList(comment, moduleName));
        moduleList.add(Arrays.asList(model, client, server));
    }

    private static void soutInfo() throws Exception {
        System.out.println();
        for (List<String> list : moduleNameList) {
            System.out.println(String.format("<!-- %s模块 -->\n<module>%s</module>", list.get(0), list.get(1)));
        }
        System.out.println();
        for (List<String> list : moduleList) {
            System.out.println(String.format("\n<dependency>\n" +
                    "    <groupId>${project.groupId}</groupId>\n" +
                    "    <artifactId>%s</artifactId>\n" +
                    "    <version>${project.version}</version>\n" +
                    "</dependency>\n" +
                    "<dependency>\n" +
                    "    <groupId>${project.groupId}</groupId>\n" +
                    "    <artifactId>%s</artifactId>\n" +
                    "    <version>${project.version}</version>\n" +
                    "</dependency>\n"+
                    "<dependency>\n" +
                    "    <groupId>${project.groupId}</groupId>\n" +
                    "    <artifactId>%s</artifactId>\n" +
                    "    <version>${project.version}</version>\n" +
                    "</dependency>", list.get(0), list.get(1), list.get(2)));
        }
        System.out.println("\n");
        for (List<String> list : moduleList) {
            System.out.println(String.format("<dependency>\n" +
                    "    <groupId>${project.groupId}</groupId>\n" +
                    "    <artifactId>%s</artifactId>\n" +
                    "</dependency>", list.get(1)));
        }
        System.out.println();

        StringBuilder sbd = new StringBuilder();
        for (List<String> list : moduleList) {
            String module = list.get(0);
            String className = capitalize(module.substring(0, module.indexOf("-model")));
            sbd.append(String.format("%sConst.MODULE_NAME, %sConst.class,\n", className, className));
        }
        sbd.delete(sbd.length() - 2, sbd.length());
        System.out.println(sbd.toString());
        System.out.println();
        Thread.sleep(20);
    }
}


class Parent {
    static void generateParent(String moduleName, String model, String client, String server, String module, String comment) {
        new File(module).mkdirs();
        String PARENT_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "    <parent>\n" +
                "        <artifactId>" + PROJECT + "</artifactId>\n" +
                "        <groupId>" + PACKAGE + "</groupId>\n" +
                "        <version>1.0-SNAPSHOT</version>\n" +
                "    </parent>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "    <artifactId>%s</artifactId>\n" +
                "    <description>%s模块</description>\n" +
                "    <packaging>pom</packaging>\n" +
                "\n" +
                "    <modules>\n" +
                "        <module>%s</module>\n" +
                "        <module>%s</module>\n" +
                "        <module>%s</module>\n" +
                "    </modules>\n" +
                "</project>\n";
        String pom = String.format(PARENT_POM, moduleName, comment, model, client, server);
        writeFile(new File(module, "pom.xml"), pom);
    }
}


class Client {
    private static final String CLIENT = "package " + PACKAGE + ".%s.client;\n" +
            "\n" +
            "import " + PACKAGE + ".%s.service.%sInterface;\n" +
            "import " + PACKAGE + ".%s.constant.%sConst;\n" +
            (fallback ? "import " + PACKAGE + ".%s.hystrix.%sFallback;\n" : "") +
            "import org.springframework.cloud.netflix.feign.FeignClient;\n" +
            "\n" +
            "/**\n" +
            " * %s相关的调用接口\n" +
            AUTHOR +
            " */\n" +
            "@FeignClient(value = %sConst.MODULE_NAME" + (fallback ? ", fallback = %sFallback.class" : "") + ")\n" +
            "public interface %sService extends %sInterface {\n" +
            "}\n";

    private static final String FALLBACK = "package " + PACKAGE + ".%s.hystrix;\n" +
            "\n" +
            "import " + PACKAGE + ".common.page.PageInfo;\n" +
            "import " + PACKAGE + ".common.page.Pages;\n" +
            "import " + PACKAGE + ".common.util.LogUtil;\n" +
            "import " + PACKAGE + ".%s.client.%sClient;\n" +
            "import org.springframework.stereotype.Component;\n" +
            "\n" +
            "/**\n" +
            " * %s相关的断路器\n" +
            AUTHOR +
            " */\n" +
            "@Component\n" +
            "public class %sFallback implements %sService {\n" +
            "\n" +
            "    @Override\n" +
            "    public PageInfo demo(String xx, Integer page, Integer limit) {\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.debug(\"调用断路器\");\n" +
            "        }\n" +
            "        return Pages.returnPage(null);\n" +
            "    }\n" +
            "}\n";

    private static final String POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <parent>\n" +
            "        <artifactId>%s</artifactId>\n" +
            "        <groupId>" + PACKAGE + "</groupId>\n" +
            "        <version>1.0-SNAPSHOT</version>\n" +
            "    </parent>\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "    <artifactId>%s-client</artifactId>\n" +
            "    <description>%s 模块中将 restful 调用封装成 FeignClient 以达到 rpc 的调用形式(需要引入 feign 包和开启 @EnableFeignClients 注解)</description>\n" +
            "\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>mall-common</artifactId>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>%s-model</artifactId>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.cloud</groupId>\n" +
            "            <artifactId>spring-cloud-starter-openfeign</artifactId>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "</project>\n";

    static void generateClient(String moduleName, String packageName, String client,
                              String module, String comment) throws IOException {
        String parentPackageName = packageName.replace("-", ".");
        String clazzName = capitalize(parentPackageName);

        File modelPath = new File(module + "/" + client + "/src/main/java");
        modelPath.mkdirs();
        String modelPom = String.format(POM, moduleName, packageName, comment, packageName);
        writeFile(new File(module + "/" + client, "pom.xml"), modelPom);

        File modelSourcePath = new File(modelPath, PACKAGE_PATH + "/" + parentPackageName.replaceAll("\\.", "/"));
        File model_client = new File(modelSourcePath, "client");
        model_client.mkdirs();

        String constModel;
        if (fallback) {
            constModel = String.format(CLIENT, parentPackageName,
                    parentPackageName, clazzName, parentPackageName, clazzName,
                    parentPackageName, clazzName, comment, clazzName, clazzName,
                    clazzName, clazzName);

            File modelHystrix = new File(modelSourcePath, "hystrix");
            modelHystrix.mkdirs();
            String interfaceModel = String.format(FALLBACK, parentPackageName,
                    parentPackageName, clazzName, comment, clazzName, clazzName);
            writeFile(new File(modelHystrix, clazzName + "Fallback.java"), interfaceModel);
        } else {
            constModel = String.format(CLIENT, parentPackageName,
                    parentPackageName, clazzName, parentPackageName, clazzName,
                    comment, clazzName, clazzName, clazzName, clazzName);
        }
        writeFile(new File(model_client, clazzName + "Service.java"), constModel);
    }
}


class Model {
    private static final String CONST = "package " + PACKAGE + ".%s.constant;\n" +
            "\n" +
            "/**\n" +
            " * %s模块相关的常数设置类\n" +
            AUTHOR +
            " */\n" +
            "public final class %sConst {\n" +
            "\n" +
            "    /** 当前模块名. 要与 bootstrap.yml 中的一致 */\n" +
            "    public static final String MODULE_NAME = \"%s\";\n" +
            "\n" +
            "    /** 当前模块说明. 当用在文档中时有用 */\n" +
            "    public static final String MODULE_INFO = MODULE_NAME + \"-%s\";\n" +
            "\n\n" +
            "    // ========== url 说明 ==========\n\n" +
            "    /** 测试地址 */\n" +
            "    public static final String %s_DEMO = MODULE_NAME + \"/demo\";\n" +
            "}\n";

    private static final String INTERFACE = "package " + PACKAGE + ".%s.service;\n" +
            "\n" +
            "import " + PACKAGE + ".common.page.PageInfo;\n" +
            "import " + PACKAGE + ".%s.constant.%sConst;\n" +
            "import org.springframework.web.bind.annotation.GetMapping;\n" +
            "import org.springframework.web.bind.annotation.RequestParam;\n" +
            "\n" +
            "/**\n" +
            " * %s相关的接口\n" +
            AUTHOR +
            " */\n" +
            "public interface %sInterface {\n" +
            "    \n" +
            "    /**\n" +
            "     * 示例接口\n" +
            "     * \n" +
            "     * @param xx 参数\n" +
            "     * @param page 当前页\n" +
            "     * @param limit 每页行数\n" +
            "     * @return 分页信息\n" +
            "     */\n" +
            "    @GetMapping(%sConst.%s_DEMO)\n" +
            "    PageInfo demo(@RequestParam(value = \"xx\", required = false) String xx,\n" +
            "                  @RequestParam(value = \"page\", required = false) Integer page,\n" +
            "                  @RequestParam(value = \"limit\", required = false) Integer limit);\n" +
            "}\n";

    private static final String POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <parent>\n" +
            "        <artifactId>%s</artifactId>\n" +
            "        <groupId>" + PACKAGE + "</groupId>\n" +
            "        <version>1.0-SNAPSHOT</version>\n" +
            "    </parent>\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "    <artifactId>%s</artifactId>\n" +
            "    <description>%s模块相关的实体</description>" +
            "\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>mall-common</artifactId>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.boot</groupId>\n" +
            "            <artifactId>spring-boot-starter-web</artifactId>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "</project>\n";

    static void generateModel(String moduleName, String packageName, String model,
                              String module, String comment) throws IOException {
        packageName = packageName.replace("-", ".");
        String clazzName = capitalize(packageName);

        File modelPath = new File(module + "/" + model + "/src/main/java");
        modelPath.mkdirs();
        String modelPom = String.format(POM, moduleName, model, comment);
        writeFile(new File(module + "/" + model, "pom.xml"), modelPom);

        File modelSourcePath = new File(modelPath, PACKAGE_PATH + "/" + packageName.replaceAll("\\.", "/"));
        File model_config = new File(modelSourcePath, "constant");
        File model_interface = new File(modelSourcePath, "service");
        model_config.mkdirs();
        model_interface.mkdirs();
        new File(modelSourcePath, "enums").mkdirs();
        new File(modelSourcePath, "model").mkdirs();
        String constModel = String.format(CONST, packageName, comment, clazzName,
                packageName, comment, clazzName.toUpperCase());
        writeFile(new File(model_config, clazzName + "Const.java"), constModel);

        String interfaceModel = String.format(INTERFACE, packageName, packageName, clazzName,
                comment, clazzName, clazzName, clazzName.toUpperCase());
        writeFile(new File(model_interface, clazzName + "Interface.java"), interfaceModel);
    }
}


class Server {
    private static final String APPLICATION = "package " + PACKAGE + ";\n" +
            "\n" +
            "import " + PACKAGE + ".common.util.A;\n" +
            "import " + PACKAGE + ".common.util.LogUtil;\n" +
            "import org.springframework.boot.SpringApplication;\n" +
            "import org.springframework.boot.autoconfigure.SpringBootApplication;\n" +
            "import org.springframework.boot.builder.SpringApplicationBuilder;\n" +
            // 2.0
            // "import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;\n" +
            "import org.springframework.boot.web.support.SpringBootServletInitializer;\n" +
            "import org.springframework.cloud.client.discovery.EnableDiscoveryClient;\n" +
            "import org.springframework.context.ApplicationContext;\n" +
            "\n" +
            "@SpringBootApplication\n" +
            "@EnableDiscoveryClient\n" +
            "public class %sApplication extends SpringBootServletInitializer {\n" +
            "\n" +
            "    @Override\n" +
            "    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {\n" +
            "        return application.sources(%sApplication.class);\n" +
            "    }\n" +
            "\n" +
            "    public static void main(String[] args) {\n" +
            "        ApplicationContext ctx = SpringApplication.run(%sApplication.class, args);\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            String[] activeProfiles = ctx.getEnvironment().getActiveProfiles();\n" +
            "            if (A.isNotEmpty(activeProfiles)) {\n" +
            "                LogUtil.ROOT_LOG.debug(\"current profile : ({})\", A.toStr(activeProfiles));\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    private static final String API_INFO_DATA = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import " + PACKAGE + ".common.json.JsonCode;\n" +
            "import " + PACKAGE + ".global.constant.Develop;\n" +
            "import com.github.liuanxin.api.annotation.EnableApiInfo;\n" +
            "import com.github.liuanxin.api.model.DocumentCopyright;\n" +
            "import com.github.liuanxin.api.model.DocumentResponse;\n" +
            "import com.google.common.collect.Lists;\n" +
            "import com.google.common.collect.Sets;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "\n" +
            "import java.util.List;\n" +
            "import java.util.Set;\n" +
            "\n" +
            "@Configuration\n" +
            "@EnableApiInfo\n" +
            "@ConditionalOnClass(DocumentCopyright.class)\n" +
            "public class %sApiInfoConfig {\n" +
            "\n" +
            "    @Value(\"${online:false}\")\n" +
            "    private boolean online;\n" +
            "\n" +
            "    @Bean\n" +
            "    public DocumentCopyright urlCopyright() {\n" +
            "        return new DocumentCopyright()\n" +
            "                .setTitle(Develop.TITLE + \" - %s\")\n" +
            "                .setTeam(Develop.TEAM)\n" +
            "                .setCopyright(Develop.COPYRIGHT)\n" +
            "                .setVersion(Develop.VERSION)\n" +
            "                .setOnline(online)\n" +
            "                .setIgnoreUrlSet(ignoreUrl())\n" +
            "                .setGlobalResponse(globalResponse());\n" +
            "    }\n" +
            "\n" +
            "    private Set<String> ignoreUrl() {\n" +
            "        return Sets.newHashSet(\"/error\");\n" +
            "    }\n" +
            "\n" +
            "    private List<DocumentResponse> globalResponse() {\n" +
            "        List<DocumentResponse> responseList = Lists.newArrayList();\n" +
            "        for (JsonCode code : JsonCode.values()) {\n" +
            "            responseList.add(new DocumentResponse(code.getFlag(), code.getMsg()));\n" +
            "        }\n" +
            "        return responseList;\n" +
            "    }\n" +
            "}\n";

    private static final String CONFIG_DATA = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import " + PACKAGE + ".common.resource.CollectTypeHandlerUtil;\n" +
            "import " + PACKAGE + ".common.resource.CollectResourceUtil;\n" +
            "import " + PACKAGE + ".common.util.A;\n" +
            "import " + PACKAGE + ".global.constant.GlobalConst;\n" +
            "import " + PACKAGE + ".%s.constant.%sConst;\n" +
            "import org.apache.ibatis.type.TypeHandler;\n" +
            "import org.springframework.core.io.Resource;\n" +
            "\n" +
            "/**\n" +
            " * %s模块的配置数据. 主要是 mybatis 的多配置目录和类型处理器\n" +
            AUTHOR +
            " */\n" +
            "final class %sConfigData {\n" +
            "\n" +
            "    private static final String[] RESOURCE_PATH = new String[] {\n" +
            "            %sConst.MODULE_NAME + \"/*.xml\",\n" +
            "            %sConst.MODULE_NAME + \"-custom/*.xml\"\n" +
            "    };\n" +
            "    /** 要加载的 mybatis 的配置文件目录 */\n" +
            "    static final Resource[] RESOURCE_ARRAY = CollectResourceUtil.resource(A.maps(\n" +
            "            %sConfigData.class, RESOURCE_PATH\n" +
            "    ));\n" +
            "    \n" +
            "    /** 要加载的 mybatis 类型处理器的目录 */\n" +
            "    static final TypeHandler[] HANDLER_ARRAY = CollectTypeHandlerUtil.typeHandler(A.maps(\n" +
            "            GlobalConst.MODULE_NAME, GlobalConst.class,\n" +
            "            %sConst.MODULE_NAME, %sConfigData.class\n" +
            "    ));\n" +
            "}\n";

    private static final String DATA_SOURCE = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import com.github.liuanxin.page.PageInterceptor;\n" +
            "import " + PACKAGE + ".common.Const;\n" +
            "import org.apache.ibatis.plugin.Interceptor;\n" +
            "import org.apache.ibatis.session.SqlSessionFactory;\n" +
            "import org.mybatis.spring.SqlSessionFactoryBean;\n" +
            "import org.mybatis.spring.SqlSessionTemplate;\n" +
            "import org.mybatis.spring.annotation.MapperScan;\n" +
            "import org.springframework.beans.factory.annotation.Autowired;\n" +
            "import org.springframework.context.annotation.Bean;\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "\n" +
            "import javax.sql.DataSource;\n" +
            "\n" +
            "/**\n" +
            " * 扫描指定目录. MapperScan 的处理类是 MapperScannerRegistrar, 其基于 ClassPathMapperScanner<br>\n" +
            " *\n" +
            " * @see org.mybatis.spring.annotation.MapperScannerRegistrar#registerBeanDefinitions\n" +
            " * @see org.mybatis.spring.mapper.MapperScannerConfigurer#postProcessBeanDefinitionRegistry\n" +
            " * @see org.mybatis.spring.mapper.ClassPathMapperScanner\n" +
            AUTHOR +
            " */\n" +
            "@Configuration\n" +
            "@MapperScan(basePackages = Const.BASE_PACKAGE)\n" +
            "public class %sDataSourceInit {\n" +
            "\n" +
            "    @Autowired\n" +
            "    private DataSource dataSource;\n" +
            "\n" +
            "    @Bean\n" +
            "    public SqlSessionFactory sqlSessionFactory() throws Exception {\n" +
            "        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();\n" +
            "        sessionFactory.setDataSource(dataSource);\n" +
            "        // 装载 xml 实现\n" +
            "        sessionFactory.setMapperLocations(%sConfigData.RESOURCE_ARRAY);\n" +
            "        // 装载 typeHandler 实现\n" +
            "        sessionFactory.setTypeHandlers(%sConfigData.HANDLER_ARRAY);\n" +
            "        // mybatis 的分页插件\n" +
            "        sessionFactory.setPlugins(new Interceptor[] { new PageInterceptor(\"mysql\") });\n" +
            "        return sessionFactory.getObject();\n" +
            "    }\n" +
            "\n" +
            "    /** 要构建 or 语句, 参考: http://www.mybatis.org/generator/generatedobjects/exampleClassUsage.html */\n" +
            "    @Bean(name = \"sqlSessionTemplate\", destroyMethod = \"clearCache\")\n" +
            "    public SqlSessionTemplate sqlSessionTemplate() throws Exception {\n" +
            "        return new SqlSessionTemplate(sqlSessionFactory());\n" +
            "    }\n" +
            "\n" +
            "    /*\n" +
            "     * 事务控制, 默认已经装载了\n" +
            "     *\n" +
            "     * @see DataSourceTransactionManagerAutoConfiguration\n" +
            "     */\n" +
            "    /*\n" +
            "    @Bean\n" +
            "    public PlatformTransactionManager transactionManager() {\n" +
            "        return new DataSourceTransactionManager(dataSource());\n" +
            "    }\n" +
            "    */\n" +
            "}\n";

    private static final String EXCEPTION = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import " + PACKAGE + ".common.exception.ForbiddenException;\n" +
            "import " + PACKAGE + ".common.exception.NotLoginException;\n" +
            "import " + PACKAGE + ".common.exception.ServiceException;\n" +
            "import " + PACKAGE + ".common.json.JsonResult;\n" +
            "import " + PACKAGE + ".common.util.A;\n" +
            "import " + PACKAGE + ".common.util.LogUtil;\n" +
            "import " + PACKAGE + ".common.util.RequestUtils;\n" +
            "import " + PACKAGE + ".common.util.U;\n" +
            "import org.springframework.beans.factory.annotation.Value;\n" +
            "import org.springframework.http.HttpStatus;\n" +
            "import org.springframework.http.ResponseEntity;\n" +
            "import org.springframework.web.HttpRequestMethodNotSupportedException;\n" +
            "import org.springframework.web.bind.MissingServletRequestParameterException;\n" +
            "import org.springframework.web.bind.annotation.ExceptionHandler;\n" +
            "import org.springframework.web.bind.annotation.RestControllerAdvice;\n" +
            "import org.springframework.web.multipart.MaxUploadSizeExceededException;\n" +
            "import org.springframework.web.servlet.NoHandlerFoundException;\n" +
            "\n" +
            "/**\n" +
            " * 处理全局异常的控制类. 如果要自定义错误处理类\n" +
            " *\n" +
            " * @see org.springframework.boot.autoconfigure.web.ErrorController\n" +
            " * @see org.springframework.boot.autoconfigure.web.ErrorProperties\n" +
            " * @see org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration\n" +
            AUTHOR +
            " */\n" +
            "@RestControllerAdvice\n" +
            "public class %sGlobalException {\n" +
            "\n" +
            "    @Value(\"${online:false}\")\n" +
            "    private boolean online;\n" +
            "\n" +
            "    /** 业务异常 */\n" +
            "    @ExceptionHandler(ServiceException.class)\n" +
            "    public ResponseEntity<JsonResult> service(ServiceException e) {\n" +
            "        String msg = e.getMessage();\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.debug(msg);\n" +
            "        }\n" +
            "        return fail(msg);\n" +
            "    }\n" +
            "    /** 未登录 */\n" +
            "    @ExceptionHandler(NotLoginException.class)\n" +
            "    public ResponseEntity<JsonResult> notLogin(NotLoginException e) {\n" +
            "        String msg = e.getMessage();\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.debug(msg);\n" +
            "        }\n" +
            "        return new ResponseEntity<>(JsonResult.notLogin(msg), HttpStatus.UNAUTHORIZED);\n" +
            "    }\n" +
            "    /** 无权限 */\n" +
            "    @ExceptionHandler(ForbiddenException.class)\n" +
            "    public ResponseEntity<JsonResult> forbidden(ForbiddenException e) {\n" +
            "        String msg = e.getMessage();\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.debug(msg);\n" +
            "        }\n" +
            "        return new ResponseEntity<>(JsonResult.notPermission(msg), HttpStatus.FORBIDDEN);\n" +
            "    }\n" +
            "\n" +
            "    @ExceptionHandler(NoHandlerFoundException.class)\n" +
            "    public ResponseEntity<JsonResult> noHandler(NoHandlerFoundException e) {\n" +
            "        bindAndPrintLog(e);\n" +
            "\n" +
            "        String msg = String.format(\"没找到(%%s %%s)\", e.getHttpMethod(), e.getRequestURL());\n" +
            "        return new ResponseEntity<>(JsonResult.notFound(msg), HttpStatus.NOT_FOUND);\n" +
            "    }\n" +
            "    @ExceptionHandler(MissingServletRequestParameterException.class)\n" +
            "    public ResponseEntity<JsonResult> missParam(MissingServletRequestParameterException e) {\n" +
            "        bindAndPrintLog(e);\n" +
            "\n" +
            "        String msg = String.format(\"缺少必须的参数(%%s), 类型(%%s)\", e.getParameterName(), e.getParameterType());\n" +
            "        return new ResponseEntity<>(JsonResult.badRequest(msg), HttpStatus.BAD_REQUEST);\n" +
            "    }\n" +
            "    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)\n" +
            "    public ResponseEntity<JsonResult> notSupported(HttpRequestMethodNotSupportedException e) {\n" +
            "        bindAndPrintLog(e);\n" +
            "\n" +
            "        String msg = \"不支持此种请求方式.\";\n" +
            "        if (!online) {\n" +
            "            msg += String.format(\" 当前(%%s), 支持(%%s)\", e.getMethod(), A.toStr(e.getSupportedMethods()));\n" +
            "        }\n" +
            "        return fail(msg);\n" +
            "    }\n" +
            "    @ExceptionHandler(MaxUploadSizeExceededException.class)\n" +
            "    public ResponseEntity<JsonResult> uploadSizeExceeded(MaxUploadSizeExceededException e) {\n" +
            "        bindAndPrintLog(e);\n" +
            "\n" +
            "        // 右移 20 位相当于除以两次 1024, 正好表示从字节到 Mb\n" +
            "        return fail(String.format(\"上传文件太大! 请保持在 %%sM 以内\", (e.getMaxUploadSize() >> 20)));\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    /** 未知的所有其他异常 */\n" +
            "    @ExceptionHandler(Throwable.class)\n" +
            "    public ResponseEntity<JsonResult> other(Throwable e) {\n" +
            "        if (LogUtil.ROOT_LOG.isErrorEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.error(\"有错误\", e);\n" +
            "        }\n" +
            "        return fail(U.returnMsg(e, online));\n" +
            "    }\n" +
            "\n" +
            "    // ==================================================\n" +
            "\n" +
            "    private void bindAndPrintLog(Exception e) {\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            // 当没有进到全局拦截器就抛出的异常, 需要这么处理才能在日志中输出整个上下文信息\n" +
            "            LogUtil.bind(RequestUtils.logContextInfo());\n" +
            "            try {\n" +
            "                LogUtil.ROOT_LOG.debug(e.getMessage(), e);\n" +
            "            } finally {\n" +
            "                LogUtil.unbind();\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "    private ResponseEntity<JsonResult> fail(String msg) {\n" +
            "        return new ResponseEntity<>(JsonResult.fail(msg), HttpStatus.INTERNAL_SERVER_ERROR);\n" +
            "    }\n" +
            "}\n";

    private static final String INTERCEPTOR = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import " + PACKAGE + ".common.util.LogUtil;\n" +
            "import " + PACKAGE + ".common.util.RequestUtils;\n" +
            "import org.springframework.web.servlet.HandlerInterceptor;\n" +
            "import org.springframework.web.servlet.ModelAndView;\n" +
            "\n" +
            "import javax.servlet.http.HttpServletRequest;\n" +
            "import javax.servlet.http.HttpServletResponse;\n" +
            "\n" +
            "/**\n" +
            " * %s模块的 web 拦截器\n" +
            AUTHOR +
            " */\n" +
            "public class %sInterceptor implements HandlerInterceptor {\n" +
            "\n" +
            "    @Override\n" +
            "    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,\n" +
            "                             Object handler) throws Exception {\n" +
            "        LogUtil.bind(RequestUtils.logContextInfo());\n" +
            "        return true;\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void postHandle(HttpServletRequest request, HttpServletResponse response,\n" +
            "                           Object handler, ModelAndView modelAndView) throws Exception {\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,\n" +
            "                                Object handler, Exception ex) throws Exception {\n" +
            "        if (ex != null) {\n" +
            "            if (LogUtil.ROOT_LOG.isDebugEnabled())\n" +
            "                LogUtil.ROOT_LOG.debug(\"request was over, but have exception: \" + ex.getMessage());\n" +
            "        }\n" +
            "        LogUtil.unbind();\n" +
            "    }\n" +
            "}\n";

    private static final String WEB_CONFIG = "package " + PACKAGE + ".%s.config;\n" +
            "\n" +
            "import " + PACKAGE + ".common.mvc.SpringMvc;\n" +
            "import " + PACKAGE + ".common.mvc.VersionRequestMappingHandlerMapping;\n" +
            "import org.springframework.context.annotation.Configuration;\n" +
            "import org.springframework.format.FormatterRegistry;\n" +
            "import org.springframework.http.converter.HttpMessageConverter;\n" +
            "import org.springframework.web.method.support.HandlerMethodArgumentResolver;\n" +
            "import org.springframework.web.servlet.config.annotation.InterceptorRegistry;\n" +
            "import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;\n" +
            "import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;\n" +
            "import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;\n" +
            "\n" +
            "import java.util.List;\n" +
            "\n" +
            "/**\n" +
            " * %s模块的配置数据. 主要是 mybatis 的多配置目录和类型处理器\n" +
            AUTHOR +
            " */\n" +
            "@Configuration\n" +
            "public class %sWebConfig extends WebMvcConfigurationSupport {\n" +
            "\n" +
            "    @Override\n" +
            "    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {\n" +
            "        return new VersionRequestMappingHandlerMapping();\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    protected void addResourceHandlers(ResourceHandlerRegistry registry) {\n" +
            // "        // 继承至 Support 之后因为处理了版本需要手动路由静态资源\n" +
            "        registry.addResourceHandler(\"/static/**\").addResourceLocations(\"classpath:/static/\");\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void addFormatters(FormatterRegistry registry) {\n" +
            "        SpringMvc.handlerFormatter(registry);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {\n" +
            "        SpringMvc.handlerConvert(converters);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {\n" +
            "        SpringMvc.handlerArgument(argumentResolvers);\n" +
            "    }\n" +
            "\n" +
            "    @Override\n" +
            "    public void addInterceptors(InterceptorRegistry registry) {\n" +
            "        registry.addInterceptor(new %sInterceptor()).addPathPatterns(\"/**\");\n" +
            "    }\n" +
            "}\n";

    private static final String SERVICE = "package " + PACKAGE + ".%s.service;\n" +
            "\n" +
            "import " + PACKAGE + ".common.json.JsonResult;\n" +
            "import " + PACKAGE + ".common.page.PageInfo;\n" +
            "import " + PACKAGE + ".common.page.Pages;\n" +
            "import " + PACKAGE + ".common.util.LogUtil;\n" +
            "import org.springframework.web.bind.annotation.GetMapping;\n" +
            "import org.springframework.web.bind.annotation.RestController;\n" +
            "\n" +
            "/**\n" +
            " * %s模块的接口实现类\n" +
            AUTHOR +
            " */\n" +
            "@RestController\n" +
            "public class %sServiceImpl implements %sInterface {\n" +
            "    \n" +
            "    @Override\n" +
            "    public PageInfo demo(String xx, Integer page, Integer limit) {\n" +
            "        if (LogUtil.ROOT_LOG.isDebugEnabled()) {\n" +
            "            LogUtil.ROOT_LOG.debug(\"调用实现类\" + xx + \", page:\" + page + \", limit:\" + limit);\n" +
            "        }\n" +
            "        return Pages.returnPage(null);\n" +
            "    }\n" +
            "\n" +
            "    @GetMapping(\"/\")\n" +
            "    public JsonResult index() {\n" +
            "        return JsonResult.success(\"%s-module\");\n" +
            "    }\n" +
            "}\n";

    private static final String APPLICATION_YML = "\n" +
            "# https://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html\n" +
            "online: false\n" +
            "\n" +
            "server.port: %s\n" +
            "\n" +
            "spring.application.name: %s\n" +
            "\n" +
            "spring:\n" +
            "  mvc.throw-exception-if-no-handler-found: true\n" +
            "  resources.add-mappings: false\n" +
            "\n" +
            "logging.config: classpath:log-dev.xml\n" +
            "\n" +
            "spring.datasource:\n" +
            "  url: jdbc:mysql://127.0.0.1:3306/cloud?useSSL=false&useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&statementInterceptors=" + PACKAGE + ".common.sql.ShowSqlInterceptor\n" +
            "  username: root\n" +
            "  password: root\n" +
            "  hikari:\n" +
            "    minimumIdle: 1\n" +
            "    maximumPoolSize: 1\n" +
            "\n" +
            "register.center: http://127.0.0.1:" + REGISTER_CENTER_PORT + "/eureka/\n" +
            "eureka:\n" +
            "  client:\n" +
            "    # 开启健康检查(需要 spring-boot-starter-actuator 包)\n" +
            "    healthcheck.enabled: true\n" +
            "    # 客户端间隔多久去拉取服务注册信息, 默认为 30 秒\n" +
            "    registry-fetch-interval-seconds: 20\n" +
            "    serviceUrl.defaultZone: ${register.center}\n" +
            "  instance:\n" +
            "    # 注册到服务器的是 ip 地址, 不要用主机名(只在开发时这样, 测试和线上还是用默认)\n" +
            "    prefer-ip-address: true\n" +
            "    # 客户端发送心跳给注册中心的频率, 默认 30 秒\n" +
            "    lease-renewal-interval-in-seconds: 20\n" +
            "    # 服务端在收到最后一个心跳后的等待时间. 超出将移除该实例, 默认 90 秒, 此值至少要大于 lease-renewal-interval-in-seconds\n" +
            "    lease-expiration-duration-in-seconds: 60\n" +
            "\n" +
            "## org.springframework.cloud.sleuth.zipkin2.ZipkinProperties\n" +
            "#spring:\n" +
            "#  zipkin.base-url: http://127.0.0.1:9411\n" +
            "#  # 抽样比例, 默认是 10%%, 如果 值是 1 则表示 100%%, 分布式追踪数据量可能会非常大\n" +
            "#  sleuth.sampler.percentage: 0.1\n";

    private static final String APPLICATION_TEST_YML = "\n" +
            "online: false\n" +
            "\n" +
            "server.port: %s\n" +
            "\n" +
            "spring.application.name: %s\n" +
            "\n" +
            "spring:\n" +
            "  mvc.throw-exception-if-no-handler-found: true\n" +
            "  resources.add-mappings: false\n" +
            "\n" +
            "logging.config: classpath:log-test.xml\n" +
            "\n" +
            "spring.datasource:\n" +
            "  url: jdbc:mysql://test_%s_db?useSSL=false&useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true&statementInterceptors=" + PACKAGE + ".common.sql.ShowSqlInterceptor\n" +
            "  username: test_%s_user\n" +
            "  password: test_%s_pass\n" +
            "  hikari:\n" +
            "    minimumIdle: 2\n" +
            "    maximumPoolSize: 5\n" +
            "    dataSourceProperties:\n" +
            "      prepStmtCacheSize: 250\n" +
            "      prepStmtCacheSqlLimit: 2048\n" +
            "      cachePrepStmts: true\n" +
            "      useServerPrepStmts: true\n" +
            "\n" +
            "register.center: http://test1:" + REGISTER_CENTER_PORT + "/eureka/,http://test2:" +
            REGISTER_CENTER_PORT + "/eureka/,http://test3:" + REGISTER_CENTER_PORT + "/eureka/\n" +
            "eureka:\n" +
            "  client:\n" +
            "    healthcheck.enabled: true\n" +
            "    registry-fetch-interval-seconds: 10\n" +
            "    serviceUrl.defaultZone: ${register.center}\n" +
            "  instance:\n" +
            "    lease-renewal-interval-in-seconds: 10\n" +
            "    lease-expiration-duration-in-seconds: 30\n" +
            "\n" +
            "#spring:\n" +
            "#  zipkin.base-url: http://127.0.0.1:9411\n" +
            "#  sleuth.sampler.percentage: 0.1\n";

    private static final String APPLICATION_PROD_YML = "\n" +
            "online: true\n" +
            "\n" +
            "server.port: %s\n" +
            "\n" +
            "spring.application.name: %s\n" +
            "\n" +
            "spring:\n" +
            "  mvc.throw-exception-if-no-handler-found: true\n" +
            "  resources.add-mappings: false\n" +
            "\n" +
            "logging.config: classpath:log-prod.xml\n" +
            "\n" +
            "spring.datasource:\n" +
            "  url: jdbc:mysql://prod_%s_db?useSSL=false&useUnicode=true&characterEncoding=utf8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&transformedBitIsBoolean=true\n" +
            "  username: prod_%s_user\n" +
            "  password: prod_%s_pass\n" +
            "  hikari:\n" +
            "    minimumIdle: 10\n" +
            "    maximumPoolSize: 30\n" +
            "    dataSourceProperties:\n" +
            "      prepStmtCacheSize: 250\n" +
            "      prepStmtCacheSqlLimit: 2048\n" +
            "      cachePrepStmts: true\n" +
            "      useServerPrepStmts: true\n" +
            "\n" +
            "register.center: http://prod1:" + REGISTER_CENTER_PORT + "/eureka/,http://prod2:" +
            REGISTER_CENTER_PORT + "/eureka/,http://prod3:" + REGISTER_CENTER_PORT + "/eureka/\n" +
            "eureka:\n" +
            "  client:\n" +
            "    healthcheck.enabled: true\n" +
            "    registry-fetch-interval-seconds: 5\n" +
            "    serviceUrl.defaultZone: ${register.center}\n" +
            "  instance:\n" +
            "    lease-renewal-interval-in-seconds: 5\n" +
            "    lease-expiration-duration-in-seconds: 15\n" +
            "\n" +
            "#spring:\n" +
            "#  zipkin.base-url: http://127.0.0.1:9411\n" +
            "#  sleuth.sampler.percentage: 0.1\n";

    private static final String CONFIG = "\n"+
            "# 当前文件是主要为了抑制 <No URLs will be polled as dynamic configuration sources> 这个警告. 无其他用处\n"+
            "# see com.netflix.config.sources.URLConfigurationSource.URLConfigurationSource()\n";

    private static final String LOG_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<configuration>\n" +
            "    <include resource=\"org/springframework/boot/logging/logback/defaults.xml\" />\n" +
            "    <property name=\"CONSOLE_LOG_PATTERN\" value=\"[%X{receiveTime}%d] [${PID:- } %t\\\\(%logger\\\\) : %p]%X{requestInfo}%n%class.%method\\\\(%file:%line\\\\)%n%m%n%n\"/>\n" +
            "    <include resource=\"org/springframework/boot/logging/logback/console-appender.xml\" />\n" +
            "\n\n" +
            "    <logger name=\"zipkin.autoconfigure\" level=\"warn\"/>\n" +
            "    <logger name=\"io.undertow\" level=\"warn\"/>\n" +
            "    <logger name=\"freemarker\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"" + PACKAGE + ".~MODULE_NAME~.repository\" level=\"warn\"/>\n" +
            "    <logger name=\"" + PACKAGE + ".common.mvc\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"com.netflix\" level=\"warn\"/>\n" +
            "    <!--<logger name=\"com.github\" level=\"warn\"/>-->\n" +
            "    <logger name=\"com.zaxxer\" level=\"warn\"/>\n" +
            "    <logger name=\"com.sun\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"org.springframework\" level=\"warn\"/>\n" +
            "    <logger name=\"org.hibernate\" level=\"warn\"/>\n" +
            "    <logger name=\"org.mybatis\" level=\"warn\"/>\n" +
            "    <logger name=\"org.apache\" level=\"warn\"/>\n" +
            "    <logger name=\"org.jboss\" level=\"warn\"/>\n" +
            "\n\n" +
            "    <root level=\"debug\">\n" +
            "        <appender-ref ref=\"CONSOLE\"/>\n" +
            "    </root>\n" +
            "</configuration>\n";

    private static final String LOG_TEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<configuration>\n" +
            "    <property name=\"FILE_PATH\" value=\"${user.home}/logs/~MODULE_NAME~-test\"/>\n" +
            "    <property name=\"SQL_PATTERN\" value=\"%d [${PID:- } %t\\\\(%logger\\\\) : %p]%n%class.%method\\\\(%file:%line\\\\)%n%m%n%n\"/>\n" +
            "    <property name=\"LOG_PATTERN\" value=\"[%X{receiveTime}%d] [${PID:- } %t\\\\(%logger\\\\) : %p]%X{requestInfo} %class{30}#%method\\\\(%file:%line\\\\)%n%m%n%n\"/>\n" +
            "\n" +
            "    <appender name=\"PROJECT\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
            "        <file>${FILE_PATH}.log</file>\n" +
            "        <!-- yyyy-MM-dd_HH 每小时建一个, yyyy-MM-dd_HH-mm 每分钟建一个 -->\n" +
            "        <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">\n" +
            "            <fileNamePattern>${FILE_PATH}-%d{yyyy-MM-dd}.log</fileNamePattern>\n" +
            "            <maxHistory>7</maxHistory>\n" +
            "        </rollingPolicy>\n" +
            "        <!-- 开启了下面的配置将会在文件达到 10MB 的时候才新建文件, 将会按上面的规则一天建一个  -->\n" +
            "        <!--<triggeringPolicy class=\"ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy\">\n" +
            "            <MaxFileSize>10MB</MaxFileSize>\n" +
            "        </triggeringPolicy>-->\n" +
            "        <encoder>\n" +
            "            <pattern>${LOG_PATTERN}</pattern>\n" +
            "        </encoder>\n" +
            "    </appender>\n" +
            "\n" +
            "    <appender name=\"SQL\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
            "        <file>${FILE_PATH}-sql.log</file>\n" +
            "        <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">\n" +
            "            <fileNamePattern>${FILE_PATH}-sql-%d{yyyy-MM-dd}.log</fileNamePattern>\n" +
            "            <maxHistory>7</maxHistory>\n" +
            "        </rollingPolicy>\n" +
            "        <encoder>\n" +
            "            <pattern>${SQL_PATTERN}</pattern>\n" +
            "        </encoder>\n" +
            "    </appender>\n" +
            "    <logger name=\"sqlLog\" level=\"debug\" additivity=\"false\">\n" +
            "        <appender-ref ref=\"SQL\" />\n" +
            "    </logger>\n" +
            "\n\n" +
            "    <logger name=\"zipkin.autoconfigure\" level=\"warn\"/>\n" +
            "    <logger name=\"io.undertow\" level=\"warn\"/>\n" +
            "    <logger name=\"freemarker\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"" + PACKAGE + ".~MODULE_NAME~.repository\" level=\"warn\"/>\n" +
            "    <logger name=\"" + PACKAGE + ".common.mvc\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"com.netflix\" level=\"warn\"/>\n" +
            "    <!--<logger name=\"com.github\" level=\"warn\"/>-->\n" +
            "    <logger name=\"com.zaxxer\" level=\"warn\"/>\n" +
            "    <logger name=\"com.sun\" level=\"warn\"/>\n" +
            "\n" +
            "    <logger name=\"org.springframework\" level=\"warn\"/>\n" +
            "    <logger name=\"org.hibernate\" level=\"warn\"/>\n" +
            "    <logger name=\"org.mybatis\" level=\"warn\"/>\n" +
            "    <logger name=\"org.apache\" level=\"warn\"/>\n" +
            "    <logger name=\"org.jboss\" level=\"warn\"/>\n" +
            "\n\n" +
            "    <root level=\"debug\">\n" +
            "        <appender-ref ref=\"PROJECT\"/>\n" +
            "    </root>\n" +
            "</configuration>\n";

    private static final String LOG_PROD_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<configuration>\n" +
            "    <property name=\"FILE_PATH\" value=\"${user.home}/logs/~MODULE_NAME~-prod\"/>\n" +
            "    <property name=\"LOG_PATTERN\" value=\"[%X{receiveTime}%d] [${PID:- } %t\\\\(%logger\\\\) : %p]%X{requestInfo} %class{30}#%method\\\\(%file:%line\\\\) %m%n%n\"/>\n" +
            "\n" +
            "    <appender name=\"PROJECT\" class=\"ch.qos.logback.core.rolling.RollingFileAppender\">\n" +
            "        <file>${FILE_PATH}.log</file>\n" +
            "        <rollingPolicy class=\"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\">\n" +
            "            <fileNamePattern>${FILE_PATH}-%d{yyyy-MM-dd}.log</fileNamePattern>\n" +
            "            <maxHistory>30</maxHistory>\n" +
            "        </rollingPolicy>\n" +
            "        <encoder>\n" +
            "            <pattern>${LOG_PATTERN}</pattern>\n" +
            "        </encoder>\n" +
            "    </appender>\n" +
            "\n" +
            "    <appender name=\"ASYNC\" class=\"ch.qos.logback.classic.AsyncAppender\">\n" +
            "        <discardingThreshold>0</discardingThreshold>\n" +
            "        <includeCallerData>true</includeCallerData>\n" +
            "        <appender-ref ref =\"PROJECT\"/>\n" +
            "    </appender>\n" +
            "\n\n" +
            "    <logger name=\"zipkin.autoconfigure\" level=\"error\"/>\n" +
            "    <logger name=\"io.undertow\" level=\"error\"/>\n" +
            "    <logger name=\"freemarker\" level=\"error\"/>\n" +
            "\n" +
            "    <logger name=\"" + PACKAGE + ".~MODULE_NAME~.repository\" level=\"error\"/>\n" +
            "    <logger name=\"" + PACKAGE + ".common.mvc\" level=\"error\"/>\n" +
            "\n" +
            "    <logger name=\"com.netflix\" level=\"error\"/>\n" +
            "    <!--<logger name=\"com.github\" level=\"error\"/>-->\n" +
            "    <logger name=\"com.zaxxer\" level=\"error\"/>\n" +
            "    <logger name=\"com.sun\" level=\"error\"/>\n" +
            "\n" +
            "    <logger name=\"org.springframework\" level=\"error\"/>\n" +
            "    <logger name=\"org.hibernate\" level=\"error\"/>\n" +
            "    <logger name=\"org.mybatis\" level=\"error\"/>\n" +
            "    <logger name=\"org.apache\" level=\"error\"/>\n" +
            "    <logger name=\"org.jboss\" level=\"error\"/>\n" +
            "\n\n" +
            "    <root level=\"info\">\n" +
            "        <appender-ref ref=\"ASYNC\"/>\n" +
            "    </root>\n" +
            "</configuration>\n";


    private static final String SERVER_POM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <parent>\n" +
            "        <artifactId>%s</artifactId>\n" +
            "        <groupId>" + PACKAGE + "</groupId>\n" +
            "        <version>1.0-SNAPSHOT</version>\n" +
            "    </parent>\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "\n" +
            "    <artifactId>%s</artifactId>\n" +
            "    <description>%s模块的服务端</description>\n" +
            "\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>mall-common</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>mall-global</artifactId>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>${project.groupId}</groupId>\n" +
            "            <artifactId>%s</artifactId>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.boot</groupId>\n" +
            "            <artifactId>spring-boot-starter-actuator</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.boot</groupId>\n" +
            "            <artifactId>spring-boot-starter-jdbc</artifactId>\n" +
            "            <exclusions>\n" +
            "                <exclusion>\n" +
            "                    <groupId>org.apache.tomcat</groupId>\n" +
            "                    <artifactId>tomcat-jdbc</artifactId>\n" +
            "                </exclusion>\n" +
            "            </exclusions>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.cloud</groupId>\n" +
            "            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>\n" +
            "        </dependency>\n" +
//            "        <dependency>\n" +
//            "            <groupId>org.springframework.cloud</groupId>\n" +
//            "            <artifactId>spring-cloud-starter-zipkin</artifactId>\n" +
//            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>com.zaxxer</groupId>\n" +
            "            <artifactId>HikariCP</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>mysql</groupId>\n" +
            "            <artifactId>mysql-connector-java</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>org.mybatis</groupId>\n" +
            "            <artifactId>mybatis</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>org.mybatis</groupId>\n" +
            "            <artifactId>mybatis-spring</artifactId>\n" +
            "        </dependency>\n" +
            "\n" +
            "        <dependency>\n" +
            "            <groupId>com.github.liuanxin</groupId>\n" +
            "            <artifactId>mybatis-page</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>com.github.liuanxin</groupId>\n" +
            "            <artifactId>mybatis-redis-cache</artifactId>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>com.github.liuanxin</groupId>\n" +
            "            <artifactId>api-document</artifactId>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "\n" +
            "    <build>\n" +
            "        <finalName>%s</finalName>\n" +
            "        <plugins>\n" +
            "            <plugin>\n" +
            "                <groupId>org.springframework.boot</groupId>\n" +
            "                <artifactId>spring-boot-maven-plugin</artifactId>\n" +
            "            </plugin>\n" +
            "        </plugins>\n" +
            "    </build>\n" +
            "</project>\n";


    private static final String TEST_ENUM_HANDLE = "package " + PACKAGE + ".%s;\n" +
            "\n" +
            "import " + PACKAGE + ".common.Const;\n" +
            "import " + PACKAGE + ".common.util.GenerateEnumHandler;\n" +
            "import " + PACKAGE + ".%s.constant.%sConst;\n" +
            "import org.junit.Test;\n" +
            "\n" +
            "/**\n" +
            " * %s模块生成 enumHandle 的工具类\n" +
            AUTHOR +
            " */\n" +
            "public class %sGenerateEnumHandler {\n" +
            "\n" +
            "    @Test\n" +
            "    public void generate() {\n" +
            "        GenerateEnumHandler.generateEnum(getClass(), Const.BASE_PACKAGE, %sConst.MODULE_NAME);\n" +
            "    }\n" +
            "}\n";

    static void generateServer(String moduleName, String packageName, String model,
                               String server, String module, String port, String comment) throws IOException {
        String parentPackageName = packageName.replace("-", ".");
        String clazzName = capitalize(parentPackageName);

        File servmallath = new File(module + "/" + server + "/src/main/java");
        servmallath.mkdirs();

        String servmallom = String.format(SERVER_POM, moduleName, server, comment, model, server + "-" + port);
        writeFile(new File(module + "/" + server, "pom.xml"), servmallom);

        File packagePath = new File(servmallath + "/" + PACKAGE_PATH);
        File sourcePath = new File(packagePath + "/" + parentPackageName.replaceAll("\\.", "/"));
        File configPath = new File(sourcePath, "config");
        File servicePath = new File(sourcePath, "service");
        configPath.mkdirs();
        servicePath.mkdirs();
        new File(sourcePath, "handler").mkdirs();
        new File(sourcePath, "repository").mkdirs();

        String application = String.format(APPLICATION, clazzName, clazzName, clazzName);
        writeFile(new File(packagePath, clazzName + "Application.java"), application);

        String apiInfo = String.format(API_INFO_DATA, parentPackageName, clazzName, comment);
        writeFile(new File(configPath, clazzName + "ApiInfoConfig.java"), apiInfo);

        String configData = String.format(CONFIG_DATA, parentPackageName, parentPackageName, clazzName, comment,
                clazzName, clazzName, clazzName, clazzName, clazzName, clazzName);
        writeFile(new File(configPath, clazzName + "ConfigData.java"), configData);

        String dataSource = String.format(DATA_SOURCE, parentPackageName, clazzName, clazzName, clazzName);
        writeFile(new File(configPath, clazzName + "DataSourceInit.java"), dataSource);

        String exception = String.format(EXCEPTION, parentPackageName, clazzName);
        writeFile(new File(configPath, clazzName + "GlobalException.java"), exception);

        String interceptor = String.format(INTERCEPTOR, parentPackageName, comment, clazzName, clazzName);
        writeFile(new File(configPath, clazzName + "Interceptor.java"), interceptor);

        String war = String.format(WEB_CONFIG, parentPackageName, comment, clazzName, clazzName);
        writeFile(new File(configPath, clazzName + "WebConfig.java"), war);

        String service = String.format(SERVICE, parentPackageName, comment, clazzName, clazzName,
                clazzName, comment, clazzName.toUpperCase(), parentPackageName);
        writeFile(new File(servicePath, clazzName + "ServiceImpl.java"), service);


        File resourcePath = new File(module + "/" + server + "/src/main/resources");
        resourcePath.mkdirs();
        new File(resourcePath, parentPackageName).mkdir();
        new File(resourcePath, parentPackageName + "-custom").mkdir();

        String applicationYml = String.format(APPLICATION_YML, port, packageName);
        writeFile(new File(resourcePath, "application.yml"), applicationYml);
        String applicationTestYml = String.format(APPLICATION_TEST_YML, port,
                packageName, packageName, packageName, packageName);
        writeFile(new File(resourcePath, "application-test.yml"), applicationTestYml);
        String applicationProdYml = String.format(APPLICATION_PROD_YML, port,
                packageName, packageName, packageName, packageName);
        writeFile(new File(resourcePath, "application-prod.yml"), applicationProdYml);

        writeFile(new File(resourcePath, "config.properties"), CONFIG);
        String logXml = LOG_XML.replaceAll("~MODULE_NAME~", parentPackageName);
        writeFile(new File(resourcePath, "log-dev.xml"), logXml);
        String testXml = LOG_TEST_XML.replaceAll("~MODULE_NAME~", parentPackageName);
        writeFile(new File(resourcePath, "log-test.xml"), testXml);
        String prodXml = LOG_PROD_XML.replaceAll("~MODULE_NAME~", parentPackageName);
        writeFile(new File(resourcePath, "log-prod.xml"), prodXml);


        File testParent = new File(module + "/" + server + "/src/test/java/" +
                PACKAGE_PATH + "/" + parentPackageName.replace('.', '/'));
        testParent.mkdirs();

        File testResource = new File(module + "/" + server + "/src/test/resources");
        testResource.mkdirs();
        writeFile(new File(testResource, packageName + ".sql"), "");

        String test = String.format(TEST_ENUM_HANDLE, parentPackageName,
                parentPackageName, clazzName, comment, clazzName, clazzName);
        writeFile(new File(testParent, clazzName + "GenerateEnumHandler.java"), test);
    }
}
