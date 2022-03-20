package com.github.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.common.util.U;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/** 性别 */
@Getter
@AllArgsConstructor
public enum Gender {

    Nil(0, ""), Male(1, "男"), Female(2, "女");

    @EnumValue
    private final int code;

    private final String value;

    public static Gender fromCode(Integer code) {
        if (U.isNotNull(code)) {
            for (Gender value : values()) {
                if (value.code == code) {
                    return value;
                }
            }
        }
        return Nil;
    }

    /** 序列化给前端时, 如果只想给前端返回数值, 去掉此方法并把注解挪到 getCode 即可 */
    @JsonValue
    public Map<String, Object> serializer() {
        return U.serializerEnum(code, value);
    }
    /** 数据反序列化. 如 male、0、男、{"code": 0, "value": "男"} 都可以反序列化为 Gender.Male 值 */
    @JsonCreator
    public static Gender deserializer(Object obj) {
        Gender gender = U.enumDeserializer(obj, Gender.class);
        return U.isNull(gender) ? Nil : gender;
    }
}
