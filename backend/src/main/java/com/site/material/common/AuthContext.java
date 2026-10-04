package com.site.material.common;

import com.site.material.domain.SysUser;

/** 当前登录用户上下文，由鉴权过滤器在请求开始时写入 */
public final class AuthContext {
    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(SysUser user) {
        HOLDER.set(user);
    }

    public static SysUser get() {
        return HOLDER.get();
    }

    public static Long userId() {
        SysUser u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
