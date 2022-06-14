package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.common.util.DesensitizationUtil;
import com.github.common.util.U;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 脱敏主要用在 日志打印 和 某些业务接口上, 当前序列化处理器用在业务接口上
 *
 * @see JsonSensitive
 */
public class JsonSensitiveSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (U.isNull(obj)) {
            gen.writeNull();
            return;
        }

        if (obj instanceof String) {
            handleString((String) obj, gen);
            return;
        }
        if (obj instanceof Number) {
            handleNumber((Number) obj, gen);
            return;
        }
        if (obj instanceof Date) {
            handleDate((Date) obj, gen, provider);
            return;
        }

        throw new RuntimeException("Annotation @JsonSensitive can not used on types other than String Number Date");
    }

    private void handleString(String value, JsonGenerator gen) throws IOException {
        if (U.isBlank(value)) {
            gen.writeString(U.EMPTY);
            return;
        }

        JsonSensitive sensitive = getAnnotationOnField(gen);
        if (U.isNotNull(sensitive)) {
            gen.writeString(DesensitizationUtil.desString(value, sensitive.start(), sensitive.end()));
        } else {
            gen.writeString(value);
        }
    }

    private void handleNumber(Number value, JsonGenerator gen) throws IOException {
        JsonSensitive sensitive = getAnnotationOnField(gen);
        if (U.isNotNull(sensitive)) {
            gen.writeObject(DesensitizationUtil.descNumber(value, sensitive.randomNumber(), sensitive.digitsNumber()));
        } else {
            gen.writeString(value.toString());
        }
    }

    private void handleDate(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Field field = getAnnotationField(gen);
        if (U.isNotNull(field)) {
            JsonSensitive sensitive = field.getAnnotation(JsonSensitive.class);
            if (U.isNotNull(sensitive)) {
                long randomDateTimeMillis = sensitive.randomDateTimeMillis();
                if (randomDateTimeMillis != 0) {
                    DesensitizationUtil.descDate(value, randomDateTimeMillis);
                }
                if (!provider.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)) {
                    JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
                    if (U.isNotNull(jsonFormat)) {
                        // @see com.fasterxml.jackson.databind.ser.std.DateSerializer
                        JsonFormat.Value format = new JsonFormat.Value(jsonFormat);
                        Locale loc = format.hasLocale() ? format.getLocale() : provider.getLocale();
                        SimpleDateFormat df = new SimpleDateFormat(format.getPattern(), loc);
                        df.setTimeZone(format.hasTimeZone() ? format.getTimeZone() : provider.getTimeZone());
                        gen.writeString(df.format(value));
                        return;
                    }
                }
            }
        }
        provider.defaultSerializeDateValue(value, gen);
    }

    private JsonSensitive getAnnotationOnField(JsonGenerator gen) {
        Field field = getAnnotationField(gen);
        return U.isNull(field) ? null : field.getAnnotation(JsonSensitive.class);
    }
    private Field getAnnotationField(JsonGenerator gen) {
        return U.getField(gen.getCurrentValue(), gen.getOutputContext().getCurrentName());
    }
}
