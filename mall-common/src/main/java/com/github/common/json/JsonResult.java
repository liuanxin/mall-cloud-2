package com.github.common.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.liuanxin.api.annotation.ApiReturn;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** <span style="color:red;">!!!此实体类请只在 Controller 中使用, 且只调用其 static 方法!!!</span> */
@Setter
@Getter
@NoArgsConstructor
public class JsonResult<T> {

    @JsonIgnore
    private int code;

    /** 返回说明. 如: 用户名密码错误, 收货地址添加成功 等 */
    @ApiReturn("返回说明. 如: 用户名密码错误, 收货地址添加成功 等")
    private String msg;

    /** 返回的数据. 具体是返回实体 {"id":1} 还是列表 [{"id":1},{"id":2}] 依具体的业务而定 */
    @ApiReturn("返回的数据. 实体 {\"id\":1} 还是列表 [{\"id\":1},{\"id\":2}] 依具体的业务而定")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    /*
    @ApiReturn("需要 app 保存到本地的值(pc 无视), 每次请求都带上, key 是" + Const.TOKEN + ", header 或 param 都可")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String token;
    */

    private JsonResult(JsonCode code, String msg) {
        this.code = code.flag;
        this.msg = msg;
    }
    private JsonResult(JsonCode code, String msg, T data) {
        this(code, msg);
        this.data = data;
    }


    // ---------- 在 service 中请只使用下面的静态方法就好了. 不要 new JsonResult()... 这样操作 ----------

    public static <T> JsonResult<T> success(String msg) {
        return new JsonResult<T>(JsonCode.SUCCESS, msg);
    }
    public static <T> JsonResult<T> success(String msg, T data) {
        return new JsonResult<T>(JsonCode.SUCCESS, msg, data);
    }

    public static <T> JsonResult<T> badRequest(String msg) {
        return new JsonResult<T>(JsonCode.BAD_REQUEST, msg);
    }

    public static <T> JsonResult<T> notLogin(String msg) {
        return new JsonResult<T>(JsonCode.NOT_LOGIN, msg);
    }

    public static <T> JsonResult<T> notPermission(String msg) {
        return new JsonResult<T>(JsonCode.NOT_PERMISSION, msg);
    }

    public static <T> JsonResult<T> notFound(String msg) {
        return new JsonResult<T>(JsonCode.NOT_FOUND, msg);
    }

    public static <T> JsonResult<T> serviceFail(String msg) {
        // return new JsonResult<T>(JsonCode.SERVICE_FAIL, msg);
        return new JsonResult<T>(JsonCode.FAIL, msg);
    }

    public static <T> JsonResult<T> fail(String msg) {
        return new JsonResult<T>(JsonCode.FAIL, msg);
    }
}
