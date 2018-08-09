package com.github.util;

import com.github.common.json.JsonUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
class BackendSessionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 默认未登录用户的 id */
    private static final Long DEFAULT_ID = 0L;
    /** 默认未登录用户的 name */
    private static final String DEFAULT_NAME = "未登录用户";


    // ========== 存放在 session 中的数据 ==========

    /** 存放在 session 中的(用户 id) */
    private Long id;
    /** 存放在 session 中的(用户名) */
    private String name;

    // ========== 存放在 session 中的数据 ==========


    /** 当前用户在指定域名下是否已登录. 已登录就返回 true */
    boolean wasLogin() {
        return !Objects.equals(DEFAULT_ID, id) && !Objects.equals(DEFAULT_NAME, name);
    }


    // 以下为静态方法


    /** 组装数据, 将用户对象跟存进 session 中的数据进行转换 */
    static <T> BackendSessionModel assemblyData(T user) {
        return JsonUtil.convert(user, BackendSessionModel.class);
    }

    /** 未登录时的默认用户信息 */
    static BackendSessionModel defaultUser() {
        return new BackendSessionModel().setId(DEFAULT_ID).setName(DEFAULT_NAME);
    }
}
