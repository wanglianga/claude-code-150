package com.site.material.security;

import com.site.material.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static AppUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u) {
            return u;
        }
        throw new BusinessException(401, "未登录");
    }

    public static String currentName() {
        return current().displayName();
    }
}
