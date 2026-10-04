package com.site.material.security;

import com.site.material.common.AuthContext;
import com.site.material.domain.SysUser;
import com.site.material.repo.SysUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简 Token 鉴权（演示用，Token 存内存）：
 * 登录返回 X-Auth-Token，后续请求在同名请求头中携带。
 * 生产环境应替换为 JWT / Spring Security。
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private final SysUserRepository userRepository;
    private final ConcurrentHashMap<String, Long> tokens = new ConcurrentHashMap<>();

    public AuthFilter(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, userId);
        return token;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.startsWith("/api/auth/")) {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("X-Auth-Token");
        Long userId = token == null ? null : tokens.get(token);
        if (userId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已失效\"}");
            return;
        }
        Optional<SysUser> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"用户不存在\"}");
            return;
        }
        try {
            AuthContext.set(user.get());
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }
}
