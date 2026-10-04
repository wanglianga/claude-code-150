package com.site.material.web;

import com.site.material.domain.SysUser;
import com.site.material.repo.SysUserRepository;
import com.site.material.security.AuthFilter;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepository;
    private final AuthFilter authFilter;

    public AuthController(SysUserRepository userRepository, AuthFilter authFilter) {
        this.userRepository = userRepository;
        this.authFilter = authFilter;
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        SysUser user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.getPassword().equals(req.password())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = authFilter.issueToken(user.getId());
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "realName", nv(user.getRealName()),
                        "role", user.getRole(),
                        "crewId", user.getCrewId() == null ? 0 : user.getCrewId(),
                        "phone", nv(user.getPhone())));
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }
}
