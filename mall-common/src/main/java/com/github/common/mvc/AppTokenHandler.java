package com.github.common.mvc;

import com.github.common.Const;
import com.github.common.encrypt.Encrypt;
import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 专门针对 app 操作的 token 处理器, 登录时生成 token, 每次请求都刷新过期时间, 删除由客户端处理 */
public final class AppTokenHandler {

    /** 生成 token 的过期时间 */
    private static final Long TOKEN_EXPIRE_TIME = 7L;
    /** 生成 token 的过期时间单位 */
    private static final TimeUnit TOKEN_EXPIRE_TIME_UNIT = TimeUnit.DAYS;

    /** 基于存进 session 的数据生成 token 返回, 登录后调用返回给 app 由其保存下来 */
    @SuppressWarnings("unchecked")
    public static <T> String generateToken(T session) {
        if (U.isNotBlank(session)) {
            Map<String, Object> jwt = JsonUtil.convert(session, Map.class);
            if (A.isNotEmpty(jwt)) {
                // 过期时间一周
                return Encrypt.jwtEncode(jwt, TOKEN_EXPIRE_TIME, TOKEN_EXPIRE_TIME_UNIT);
            }
        }
        return U.EMPTY;
    }

    /** 重置 token 的过期时间, 每次访问时都应该调用此方法 */
    public static String resetTokenExpireTime() {
        String token = getToken();
        if (U.isNotBlank(token)) {
            Map<String, Object> session = Encrypt.jwtDecode(token);
            if (A.isNotEmpty(session)) {
                return Encrypt.jwtEncode(session, TOKEN_EXPIRE_TIME, TOKEN_EXPIRE_TIME_UNIT);
            }
        }
        return U.EMPTY;
    }

    /** 从请求中获取 token 数据 */
    private static String getToken() {
        return RequestUtils.getHeaderOrParam(Const.TOKEN);
    }

    /** 从 token 中读 session 信息 */
    public static <T> T getSessionInfoWithToken(Class<T> clazz) {
        String token = getToken();
        if (U.isNotBlank(token)) {
            Map<String, Object> session = Encrypt.jwtDecode(token);
            if (A.isNotEmpty(session)) {
                return JsonUtil.convert(session, clazz);
            }
        }
        return null;
    }
}
