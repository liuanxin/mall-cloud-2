package com.github.common.xml;

import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;

public class XmlUtil {

    /*
    使用 JacksonXml 可以像 json 一样使用相关的 api
    ObjectMapper RENDER = new XmlMapper();
    // object to xml
    String xml = RENDER.writeValueAsString(request);
    // xml to object
    Parent<Child> parent = RENDER.readValue(xml, new TypeReference<Parent<Child>>() {});

    但是需要引入一个包
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
        <version>...</version>
    </dependency>
    */

    private static final int MAX_LEN = 500;
    private static final int LEFT_RIGHT_LEN = 100;

    public static <T> String toXml(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return convertXml(obj);
        } catch (Exception e) {
            String msg = String.format("Object(%s) to xml exception", U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN));
            throw new RuntimeException(msg, e);
        }
    }
    private static <T> String convertXml(T obj) throws Exception {
        try (StringWriter writer = new StringWriter()) {
            // 在属性上标 @XmlJavaTypeAdapter(Cdata.Adapter.class) 在值的前后包值
            // 利用动态代理, 避免想输出成 <![CDATA[ abc ]]> 时却显示成了 &lt;![CDATA[ abc ]]&gt; 的问题
            XMLStreamWriter streamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(writer);
            XMLStreamWriter stream = (XMLStreamWriter) Proxy.newProxyInstance(
                    streamWriter.getClass().getClassLoader(),
                    streamWriter.getClass().getInterfaces(),
                    new Cdata.Handler(streamWriter)
            );
            Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
            // marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 不输出 <?xml version="1.0" encoding="UTF-8" standalone="yes"?> 头
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.marshal(obj, stream);
            return writer.toString();
        }
    }
    public static <T> String toXmlNil(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return convertXml(obj);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("Object(%s) to xml exception", U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN)), e);
            }
            return null;
        }
    }

    public static <T> T toObject(String xml, Class<T> clazz) {
        if (U.isBlank(xml)) {
            return null;
        }
        try {
            return convertObject(xml, clazz);
        } catch (Exception e) {
            String msg = String.format("xml(%s) to Object(%s) exception", U.toStr(xml, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName());
            throw new RuntimeException(msg, e);
        }
    }
    @SuppressWarnings("unchecked")
    private static <T> T convertObject(String xml, Class<T> clazz) throws Exception {
        return (T) JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(new StringReader(xml));
    }
    public static <T> T toObjectNil(String xml, Class<T> clazz) {
        if (U.isBlank(xml)) {
            return null;
        }
        try {
            return convertObject(xml, clazz);
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("xml(%s) to Object(%s) exception",
                        U.toStr(xml, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
            }
            return null;
        }
    }
}
