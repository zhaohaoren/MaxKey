package com.example.oidcdemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 将 application.yml 中 demo.* 配置绑定成强类型 Java 对象。 */
@ConfigurationProperties(prefix = "demo")
public class OidcDemoProperties {
    /** 登录成功和本地注销后跳转的 Vue 首页。 */
    private String frontendUrl;
    private final Oidc oidc = new Oidc();

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public Oidc getOidc() {
        return oidc;
    }

    public static class Oidc {
        /** 预期 Token 签发者，对应 id_token 的 iss Claim。 */
        private String issuer;
        /** 浏览器登录并取得 authorization code 的端点。 */
        private String authorizationUri;
        /** 后端使用 code 换取 Token 的端点。 */
        private String tokenUri;
        /** 使用 access_token 获取最终用户 Claims 的端点。 */
        private String userInfoUri;
        /** 获取 id_token 验签公钥集合的端点。 */
        private String jwkSetUri;

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getAuthorizationUri() {
            return authorizationUri;
        }

        public void setAuthorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
        }

        public String getTokenUri() {
            return tokenUri;
        }

        public void setTokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }

        public String getJwkSetUri() {
            return jwkSetUri;
        }

        public void setJwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
        }
    }
}
