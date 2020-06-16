package com.github.web;

import org.junit.Test;

public class ConvertTest {

    @Test
    public void toDto() {
        String model = "private Long id;\n" +
                "\n" +
                "    /** 用户名 --> user_name */\n" +
                "    private String userName;\n" +
                "\n" +
                "    /** 创建时间 --> create_time */\n" +
                "    private Date createTime;\n" +
                "\n" +
                "    /** 最后更新时间 --> update_time */\n" +
                "    private Date updateTime;\n";
        for (String s : model.trim().split("\n")) {
            String trim = s.trim();
            if (trim.startsWith("/**")) {
                System.out.println("@ApiParam(\"" + trim.substring(4, trim.indexOf(" --> ")) + "\")");
            } else if (trim.startsWith("private Date")) {
                System.out.println("@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\", timezone = \"GMT+8\")\n" + trim);
            } else {
                System.out.println(trim);
            }
        }
    }

    @Test
    public void toVo() {
        String model = "private Long id;\n" +
                "\n" +
                "    /** 用户名 --> user_name */\n" +
                "    private String userName;\n" +
                "\n" +
                "    /** 创建时间 --> create_time */\n" +
                "    private Date createTime;\n" +
                "\n" +
                "    /** 最后更新时间 --> update_time */\n" +
                "    private Date updateTime;\n";
        for (String s : model.trim().split("\n")) {
            String trim = s.trim();
            if (trim.startsWith("/**")) {
                System.out.println("@ApiReturn(\"" + trim.substring(4, trim.indexOf(" --> ")) + "\")");
            } else if (trim.startsWith("private Date")) {
                System.out.println("@JsonFormat(pattern = \"yyyy-MM-dd HH:mm:ss\", timezone = \"GMT+8\")\n" + trim);
            } else {
                System.out.println(trim);
            }
        }
    }
}
