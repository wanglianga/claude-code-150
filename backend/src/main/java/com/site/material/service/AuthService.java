package com.site.material.service;

import com.site.material.common.BusinessException;
import com.site.material.domain.User;
import com.site.material.dto.Requests.LoginRequest;
import com.site.material.repo.UserRepository;
import com.site.material.security.JwtService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final JwtService jwt;

    public AuthService(UserRepository userRepo, JwtService jwt) {
        this.userRepo = userRepo;
        this.jwt = jwt;
    }

    public Map<String, Object> login(LoginRequest req) {
        User u = userRepo.findById(req.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
        if (!req.password().equals(u.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwt.generate(u.getUsername(), u.getRole(), u.getDisplayName());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("username", u.getUsername());
        out.put("displayName", u.getDisplayName());
        out.put("role", u.getRole());
        out.put("crewId", u.getCrewId());
        return out;
    }
}
