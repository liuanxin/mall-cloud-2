package com.github.common.util;

import com.github.common.xml.Cdata;
import com.github.common.xml.XmlUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Test;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class XmlTest {

    @Test
    public void test() {
        String xml = XmlUtil.toXmlNil(new P1(123L, "aaa<abc>", new Date(), Arrays.asList(
                new C1(222L, "bbb"), new C1(333L, "ccc")
        )));
        System.out.println("xml: " + xml);
        System.out.println("-----");
        P1 user = XmlUtil.toObjectNil(xml, P1.class);
        System.out.println("obj: " + user);

        System.out.println("\n-------------------\n");

        xml = XmlUtil.toXmlNil(new P2(123L, "aaa", new C2(222L, "bbb")));
        System.out.println("xml: " + xml);
        System.out.println("-----");
        P2 p2 = XmlUtil.toObjectNil(xml, P2.class);
        System.out.println("obj: " + p2);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement                       // 这个注解表示这是一个根, 必须要有
    @XmlAccessorType(XmlAccessType.FIELD) // 自定义属性名需要添加这个注释
    public static class P1 {
        @XmlAttribute               // 这个属性标在 <p id="xxx">...
        private Long id;
        @XmlJavaTypeAdapter(Cdata.Adapter.class)
        private String name;
        private Date time;
        @XmlElement(name = "infos") // 自定义属性名
        private List<C1> infoList;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class C1 {
        private Long id;
        private String nick;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @XmlRootElement(name = "xml")
    public static class P2 {
        private Long id;
        private String name;
        private C2 info;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class C2 {
        private Long id;
        private String nick;
    }


    /*
    // 下面的方式也可以, 但是需要引一个包
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
        <version>...</version>
    </dependency>

    private static final TypeReference<Parent<Child>> TYPE = new TypeReference<Parent<Child>>() {};

    @Test
    public void jacksonXml() {
        Date now = new Date();
        Parent<Child> request = new Parent<>("attr", 123L, "abc", now, Arrays.asList(
                new Child("child", 321L, "xyz", now),
                new Child("cd", 321123L, "xyz-abc", now)
        ));

        ObjectMapper RENDER = new XmlMapper();
        String xml = RENDER.writeValueAsString(request);
        Parent<Child> parent = RENDER.readValue(xml, new TypeReference<Parent<Child>>() {});
        System.out.println(parent);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JacksonXmlRootElement(localName = "parent")
    public static class Parent<T> {
        @JacksonXmlProperty(isAttribute = true)
        private String service;

        private Long id;
        private String name;
        private Date time;

        @JacksonXmlProperty(localName = "list")
        private List<T> content;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Child {
        @JacksonXmlProperty(isAttribute = true)
        private String service;

        private Long id;
        private String name;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date time;
    }
    */
}
