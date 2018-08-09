package com.github.common.util;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** 自动生成 mybatis 的 enum handle */
public class GenerateEnumHandler {

    private static final String PLACEHOLDER = "CustomEnumClass";

    @SuppressWarnings("ConstantConditions")
    public static void generateEnum(Class clazz, String packageName, String moduleName) {
        String packageAndModule = packageName + "." + moduleName;

        String packName = packageAndModule + ".enums";
        URL url = clazz.getClassLoader().getResource(packName.replace(".", "/"));
        if (url == null) {
            System.out.println("在 (" + packName + ") 目录下没有找到枚举, 忽略");
            return;
        }

        String generate = packageAndModule + ".handler";
        String templateInfo = "package " + generate + ";\n" +
                "\n" +
                "import " + packName + "." + PLACEHOLDER + ";\n" +
                "import " + packageName + ".common.util.U;\n" +
                "import org.apache.ibatis.type.BaseTypeHandler;\n" +
                "import org.apache.ibatis.type.JdbcType;\n" +
                "\n" +
                "import java.sql.CallableStatement;\n" +
                "import java.sql.PreparedStatement;\n" +
                "import java.sql.ResultSet;\n" +
                "import java.sql.SQLException;\n" +
                "\n" +
                "/**\n" +
                " * 当前 handle 将会被装载进 mybatis 的运行上下文中去.\n" +
                " *\n" +
                " * @see org.apache.ibatis.type.TypeHandlerRegistry\n" +
                " * @see org.apache.ibatis.type.EnumTypeHandler\n" +
                " * @see org.apache.ibatis.type.EnumOrdinalTypeHandler\n" +
                " */\n" +
                "public class " + PLACEHOLDER + "Handler extends BaseTypeHandler<" + PLACEHOLDER + "> {\n" +
                "\n" +
                "    @Override\n" +
                "    public void setNonNullParameter(PreparedStatement ps, int i, " + PLACEHOLDER + " parameter,\n" +
                "                                    JdbcType jdbcType) throws SQLException {\n" +
                // "        // 使用 getCode 返回的 int 值来存储\n" +
                "        ps.setInt(i, parameter.getCode());\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + PLACEHOLDER + " getNullableResult(ResultSet rs, String columnName) throws SQLException {\n" +
                "        return U.toEnum(" + PLACEHOLDER + ".class, rs.getObject(columnName));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + PLACEHOLDER + " getNullableResult(ResultSet rs, int columnIndex) throws SQLException {\n" +
                "        return U.toEnum(" + PLACEHOLDER + ".class, rs.getObject(columnIndex));\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public " + PLACEHOLDER + " getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {\n" +
                "        return U.toEnum(" + PLACEHOLDER + ".class, cs.getObject(columnIndex));\n" +
                "    }\n" +
                "}\n";

        File packageParent = new File(url.getPath());
        if (packageParent.isDirectory() && A.isNotEmpty(packageParent.listFiles())) {
            int count = 0;
            for (File file : packageParent.listFiles()) {
                String className = packName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> aClass = Class.forName(className);
                    if (aClass != null && aClass.isEnum()) {
                        String clazzName = aClass.getSimpleName();
                        String parent = clazz.getClassLoader().getResource("").getPath()
                                .replace("target/classes", "src/main/java")
                                .replace("target/test-classes", "src/main/java")
                                .replace(packName.replace(".", "/"), generate.replace(".", "/"));
                        if (!parent.contains(generate.replace(".", "/"))) {
                            parent += generate.replace(".", "/");
                        }
                        new File(parent).mkdirs();

                        File writeFile = new File(parent, clazzName + "Handler.java");
                        if (!writeFile.exists()) {
                            Files.write(templateInfo.replaceAll(PLACEHOLDER, clazzName).getBytes(StandardCharsets.UTF_8), writeFile);
                            System.out.println("生成文件: " + writeFile.getPath());
                            count += 1;
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    System.out.println("找不到类(" + className + ")");
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("写文件时出了问题");
                }
            }
            System.out.println("生成了 " + count + " 个文件");
        }
    }
}
