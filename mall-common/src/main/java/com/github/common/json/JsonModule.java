package com.github.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.common.date.DateUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;

public class JsonModule {

    /** 序列化 BigDecimal 小数位不足 2 位的返回 2 位 */
    public static SimpleModule bigDecimalSerializer() {
        return new SimpleModule().addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
            @Override
            public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                String data;
                if (U.isNull(value)) {
                    data = U.EMPTY;
                } else if (value.scale() < 2) {
                    data = new DecimalFormat("0.00").format(value);
                } else {
                    data = value.toString();
                }
                gen.writeString(data);
            }
        });
    }

    /** 字符串脱敏 */
    public static SimpleModule stringDesensitization(int max, int len) {
        return new SimpleModule().addSerializer(String.class, new JsonSerializer<String>() {
            @Override
            public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (U.isNull(value)) {
                    return;
                }
                if ("".equals(value.trim())) {
                    gen.writeString(value);
                    return;
                }
                String key = gen.getOutputContext().getCurrentName();
                if (U.isBlank(key)) {
                    gen.writeString(value);
                    return;
                }

                String data;
                if ("password".equalsIgnoreCase(key)) {
                    data = "***";
                } else {
                    int length = value.length();
                    data = (length <= max) ? value : (value.substring(0, len) + " *** " + value.substring(length - len));
                }
                gen.writeString(data);
            }
        });
    }


    // ======================================== 上面是序列化, 下面是反序列化 ========================================


    /** 反序列化 Date, 序列化使用全局配置, 或者属性上的 @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") 注解 */
    public static SimpleModule dateDeserializer() {
        return new SimpleModule().addDeserializer(Date.class, new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                return DateUtil.parse(p.getText().trim());
            }
        });
    }
}
