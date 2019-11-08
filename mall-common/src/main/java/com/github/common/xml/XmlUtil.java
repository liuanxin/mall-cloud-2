package com.github.common.xml;

import com.github.common.util.LogUtil;
import com.github.common.util.U;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringReader;
import java.io.StringWriter;

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
        try (StringWriter writer = new StringWriter()) {
            Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (Exception e) {
            String msg = String.format("Object(%s) to xml exception", U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN));
            throw new RuntimeException(msg, e);
        }
    }
    public static <T> String toXmlNil(T obj) {
        if (obj == null) {
            return null;
        }
        try (StringWriter writer = new StringWriter()) {
            Marshaller marshaller = JAXBContext.newInstance(obj.getClass()).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.marshal(obj, writer);
            return writer.toString();
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("Object(%s) to xml exception", U.toStr(obj, MAX_LEN, LEFT_RIGHT_LEN)), e);
            }
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T toObject(String xml, Class<T> clazz) {
        if (xml == null || "".equalsIgnoreCase(xml.trim())) {
            return null;
        }
        try {
            return (T) JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(new StringReader(xml));
        } catch (Exception e) {
            String msg = String.format("xml(%s) to Object(%s) exception", U.toStr(xml, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName());
            throw new RuntimeException(msg, e);
        }
    }
    @SuppressWarnings("unchecked")
    public static <T> T toObjectNil(String xml, Class<T> clazz) {
        if (xml == null || "".equalsIgnoreCase(xml.trim())) {
            return null;
        }
        try {
            return (T) JAXBContext.newInstance(clazz).createUnmarshaller().unmarshal(new StringReader(xml));
        } catch (Exception e) {
            if (LogUtil.ROOT_LOG.isInfoEnabled()) {
                LogUtil.ROOT_LOG.info(String.format("xml(%s) to Object(%s) exception",
                        U.toStr(xml, MAX_LEN, LEFT_RIGHT_LEN), clazz.getName()), e);
            }
            return null;
        }
    }
}
