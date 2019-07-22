package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 返回码. 前端基于此进行相应的页面跳转, 通常会有 渲染数据、输出返回描述、导到登录页、不进行任务处理 这几种
 *
 * @see org.springframework.http.HttpStatus
 */
public enum JsonCode {

    // 一般来说, 返回编码就用在 http response code 上就好了, 当需要前端来变更页面逻辑时才需要添加
    // 比如下面的 400 403 404 对于前端来说都是输出 msg, 因此可以都用 500 来返回

    /** 将 data 解析后渲染页面(依业务而定, 也可能显示 msg 给用户看, 如 收货地址添加成功 这种) */
    SUCCESS(200, "成功"),

    // /** 参数有误(客户端错误) */
    // BAD_REQUEST(400, "参数有误"),

    /** 未登录(客户端错误) */
    NOT_LOGIN(401, "未登录"),

    // /** 无权限(客户端错误) */
    // NOT_PERMISSION(403, "无权限"),

    // /** 不需要额外处理(客户端错误) */
    // NOT_FOUND(404, "未找到相应处理"),

    /** 内部错误、业务异常(服务端错误) */
    FAIL(500, "内部错误、业务异常")

    // , SERVICE_FAIL(1000, "xxx 业务异常")
    ;

    int code;
    String value;
    JsonCode(int code, String value) {
        this.code = code;
        this.value = value;
    }

    public int getCode() {
        return code;
    }
    public String getValue() {
        return value;
    }


    @JsonValue
    public int serializer() {
        return code;
    }
    @JsonCreator
    public static JsonCode deserializer(Object obj) {
        if (obj != null) {
            String des = obj.toString();
            for (JsonCode jsonCode : values()) {
                if (des.equals(String.valueOf(jsonCode.code))) {
                    return jsonCode;
                }
                if (des.equalsIgnoreCase(jsonCode.name())) {
                    return jsonCode;
                }
            }
        }
        return SUCCESS;
    }
}
