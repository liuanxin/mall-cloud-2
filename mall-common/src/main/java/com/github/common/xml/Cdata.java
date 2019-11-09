package com.github.common.xml;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class Cdata {

    private static final String START = "<![CDATA[";
    private static final String END = "]]>";

    public static class Adapter extends XmlAdapter<String, String> {
        @Override
        public String marshal(String arg0) throws Exception {
            return START + arg0 + END;
        }

        @Override
        public String unmarshal(String arg0) throws Exception {
            return arg0;
        }
    }


    public static class Handler implements InvocationHandler {
        private XMLStreamWriter writer;
        Handler(XMLStreamWriter writer) {
            this.writer = writer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("writeCharacters".equals(method.getName()) && method.getParameterTypes()[0] == char[].class) {
                String text = new String((char[]) args[0]);
                if (text.startsWith(START) && text.endsWith(END)) {
                    writer.writeCData(text.substring(START.length(), text.length() - END.length()));
                    return null;
                }
            }
            return method.invoke(writer, args);
        }
    }
}
