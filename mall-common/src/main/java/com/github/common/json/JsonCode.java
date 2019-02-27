package com.github.common.json;

/**
 * 返回码. 前端基于此进行相应的页面跳转, 通常会有 渲染数据、输出返回描述、导到登录页、不进行任务处理 这几种
 *
 * @see org.springframework.http.HttpStatus
 */
public enum JsonCode {

    /** 将 data 解析后渲染页面(依业务而定, 也可能显示 msg 给用户看, 如 收货地址添加成功 这种) */
    SUCCESS(200, "成功. 操作数据或显示 msg 给用户看, 依具体的业务而定"),

    /** 参数有误(客户端错误) */
    BAD_REQUEST(400, "参数有误(输出 msg 即可)"),

    /** 未登录(客户端错误) */
    NOT_LOGIN(401, "未登录, 导到登录页"),

    /** 无权限(客户端错误) */
    NOT_PERMISSION(403, "无权限(输出 msg 即可)"),

    /** 不需要额外处理(客户端错误) */
    NOT_FOUND(404, "未找到相应处理(不需要处理)"),

    // /** 业务异常 */
    // SERVICE_FAIL(1000, "业务异常"),

    /** 内部错误、业务异常(服务端错误) */
    FAIL(500, "内部错误、业务异常(输出 msg 即可)");

    int flag;
    String msg;
    JsonCode(int flag, String msg) {
        this.flag = flag;
        this.msg = msg;
    }

    public int getFlag() {
        return flag;
    }
    public String getMsg() {
        return msg;
    }
}
