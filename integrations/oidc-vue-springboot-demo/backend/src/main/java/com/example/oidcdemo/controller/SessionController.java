package com.example.oidcdemo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

    @GetMapping("/api/public")
    public Map<String, Object> publicInfo() {
        return Map.of("message", "后端服务已启动");
    }

    @GetMapping("/api/me")
    public Map<String, Object> currentUser(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("subject", user.getSubject());
        result.put("claims", user.getClaims());
        return result;
    }
}
