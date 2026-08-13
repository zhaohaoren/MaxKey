package com.example.oidcdemo.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供给 Vue 的会话状态接口；浏览器不会直接读取或保存 OIDC Token。 */
@RestController
public class SessionController {

    /** 无需登录的后端存活检查。 */
    @GetMapping("/api/public")
    public Map<String, Object> publicInfo() {
        return Map.of("message", "后端服务已启动");
    }

    /**
     * 返回 Spring Security 已验证的 OIDC 用户。
     * OidcUser 来自服务端 Session，其中 Claims 已经过 issuer、audience、nonce 和签名校验。
     */
    @GetMapping("/api/me")
    public Map<String, Object> currentUser(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("authenticated", true);
        result.put("subject", user.getSubject());
        result.put("claims", user.getClaims());
        return result;
    }
}
