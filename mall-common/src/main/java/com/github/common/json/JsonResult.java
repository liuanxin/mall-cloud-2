package com.github.common.json;

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

    // 应该只有响应编码就可以了, 当前实体表示处理成功后的返回, 200 以外的响应编码统一处理
    // @ApiReturn("返回码")
    // private int code;

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

    private JsonResult(String msg) {
        this.msg = msg;
    }
    private JsonResult(String msg, T data) {
        this.msg = msg;
        this.data = data;
    }


    // --------------------

    public static <T> JsonResult<T> success(String msg) {
        return new JsonResult<T>(msg);
    }

    public static <T> JsonResult<T> success(String msg, T data) {
        return new JsonResult<T>(msg, data);
    }
}
