package com.github.util;

import com.github.common.json.JsonUtil;
import com.github.common.util.A;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
class ManagerSessionModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 默认未登录用户的 id */
    private static final Long DEFAULT_ID = 0L;
    /** 默认未登录用户的 name */
    private static final String DEFAULT_NAME = "未登录用户";
    /** 超级管理员账号 */
    private static final List<String> SUPER_USER = Arrays.asList("admin", "root");


    // ========== 存放在 session 中的数据 ==========

    /** 用户 id */
    private Long id;
    /** 用户名 */
    private String userName;
    /** 权限列表 */
    private List<Permission> permissionList;

    // ========== 存放在 session 中的数据 ==========


    @Setter
    @Getter
    @NoArgsConstructor
    @Accessors(chain = true)
    private static class Permission implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 权限路径 */
        private String url;
        /** 权限方法, 包括(get,head,post,put,delete)五种, 多个用逗号隔开 */
        private String method;
    }


    private boolean wasLogin() {
        return !DEFAULT_ID.equals(id) && !DEFAULT_NAME.equals(userName);
    }
    boolean notLogin() {
        return !wasLogin();
    }

    private boolean wasSuper() {
        return SUPER_USER.contains(userName);
    }
    boolean notSuper() {
        return !wasSuper();
    }

    private boolean wasPermission(String url, String method) {
        if (A.isNotEmpty(permissionList)) {
            for (Permission permission : permissionList) {
                String permissionUrl = permission.getUrl();
                String permissionMethod = permission.getMethod();

                // 如果配置的 url 是 /user/*, 传进来的是 /user/info 也可以通过, 通配 或 全字
                boolean matchUrl = permissionUrl.endsWith("/*") && url.startsWith(permissionUrl.replace("*", ""));
                boolean urlCheck = matchUrl || url.equals(permissionUrl);

                // 如果配置的 method 是 *, 传进来的是 GET 也可以通过, 通配 或 全字
                boolean methodCheck = (("*").equals(permissionMethod) || permissionMethod.contains(method));

                // url 和 method 都通过才表示有访问权限
                if (urlCheck && methodCheck) {
                    return true;
                }
            }
        }
        return false;
    }
    boolean notPermission(String url, String method) {
        return !wasPermission(url, method);
    }


    // 以下为静态方法


    static <T, P> ManagerSessionModel assemblyData(T account, List<P> permissions) {
        ManagerSessionModel sessionModel = JsonUtil.convert(account, ManagerSessionModel.class);
        sessionModel.setPermissionList(JsonUtil.convertList(permissions, Permission.class));
        return sessionModel;
    }

    static ManagerSessionModel defaultUser() {
        return new ManagerSessionModel().setId(DEFAULT_ID).setUserName(DEFAULT_NAME);
    }
}
