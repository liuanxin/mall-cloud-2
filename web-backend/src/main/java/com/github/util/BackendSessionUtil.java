package com.github.util;

import com.github.common.json.JsonUtil;
import com.github.common.mvc.AppTokenHandler;
import com.github.common.util.LogUtil;
import com.github.common.util.RequestUtils;
import com.github.common.util.U;

/** !!! 操作 session 都基于此, 其他地方不允许操作! 避免 session 被滥用 !!! */
public class BackendSessionUtil {

    /** 放在 session 里的图片验证码 key */
    private static final String CODE = BackendSessionUtil.class.getName() + "-CODE";
    /** 放在 session 里的用户 的 key */
    private static final String USER = BackendSessionUtil.class.getName() + "-USER";

    /** 将图片验证码的值放入 session */
    public static void putImageCode(String code) {
        RequestUtils.getSession().setAttribute(CODE, code);
        if (LogUtil.ROOT_LOG.isDebugEnabled()) {
            LogUtil.ROOT_LOG.debug("put image code({}) in session({})", code, RequestUtils.getSession().getId());
        }
    }
    /** 验证图片验证码 */
    public static boolean checkImageCode(String code) {
        if (U.isBlank(code)) {
            return false;
        }

        Object securityCode = RequestUtils.getSession().getAttribute(CODE);
        return securityCode != null && code.equalsIgnoreCase(securityCode.toString());
    }

    /** 登录之后调用此方法, 将 用户信息 放入 session, app 需要将返回的数据保存到本地 */
    public static <T> String whenLogin(T user) {
        if (U.isNotBlank(user)) {
            BackendSessionModel sessionModel = BackendSessionModel.assemblyData(user);
            if (U.isNotBlank(sessionModel)) {
                if (LogUtil.ROOT_LOG.isDebugEnabled()) {
                    LogUtil.ROOT_LOG.debug("put ({}) in session({})",
                            JsonUtil.toJson(sessionModel), RequestUtils.getSession().getId());
                }
                RequestUtils.getSession().setAttribute(USER, sessionModel);
                return AppTokenHandler.generateToken(sessionModel);
            }
        }
        return U.EMPTY;
    }


    /** 获取用户信息. 没有则使用默认信息 */
    private static BackendSessionModel getSessionInfo() {
        // 从 token 中读, 为空再从 session 中读
        BackendSessionModel sessionModel = AppTokenHandler.getSessionInfoWithToken(BackendSessionModel.class);
        if (U.isBlank(sessionModel)) {
            sessionModel = (BackendSessionModel) RequestUtils.getSession().getAttribute(USER);
        }
        // 为空则使用默认值
        return sessionModel == null ? BackendSessionModel.defaultUser() : sessionModel;
    }

    /** 从 session 中获取用户 id */
    public static Long getUserId() {
        return getSessionInfo().getId();
    }

    /** 从 session 中获取用户名 */
    public static String getUserName() {
        return getSessionInfo().getName();
    }

    public static String getUserInfo() {
        return getSessionInfo().userInfo();
    }

    /** 验证登录, 未登录则抛出异常 */
    public static void checkLogin() {
        if (!getSessionInfo().wasLogin()) {
            U.notLoginException();
        }
    }

    /** 退出登录时调用. 清空 session */
    public static void signOut() {
        RequestUtils.getSession().invalidate();
    }
}
