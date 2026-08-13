package com.example.oidcdemo.config;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpStatus;

/**
 * Spring Security 的 OIDC Client 配置。
 *
 * <p>标准 OIDC Provider 通常只需要配置 issuer-uri，Spring 会通过 Discovery
 * 自动发现授权、Token、UserInfo 和 JWKS 端点。MaxKey 当前的 Discovery 路径与
 * issuer 路径不完全一致，因此本示例显式构造 ClientRegistration。</p>
 */
@Configuration
@EnableConfigurationProperties({OidcDemoProperties.class, OAuth2ClientProperties.class})
public class SecurityConfiguration {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OidcDemoProperties properties,
                                                              OAuth2ClientProperties clientProperties) {
        OAuth2ClientProperties.Registration maxKey = clientProperties.getRegistration().get("maxkey");
        if (maxKey == null) {
            throw new IllegalStateException("Missing spring.security.oauth2.client.registration.maxkey configuration");
        }

        // registrationId=maxkey 决定默认登录入口为 /oauth2/authorization/maxkey。
        ClientRegistration registration = ClientRegistration.withRegistrationId("maxkey")
                // Client 身份：id 可公开，secret 只能保存在后端。
                .clientId(maxKey.getClientId())
                .clientSecret(maxKey.getClientSecret())
                // MaxKey 要求换 Token 时在表单正文中提交 client_id/client_secret。
                .clientAuthenticationMethod(new ClientAuthenticationMethod(maxKey.getClientAuthenticationMethod()))
                // 使用后端 Web 应用推荐的 Authorization Code Flow。
                .authorizationGrantType(new AuthorizationGrantType(maxKey.getAuthorizationGrantType()))
                // 授权完成后 MaxKey 回到这里；必须与 MaxKey 管理端登记值完全一致。
                .redirectUri(maxKey.getRedirectUri())
                // openid 是 OIDC 必需值；profile/email 用于申请标准用户 Claims。
                .scope(maxKey.getScope())
                // 浏览器登录授权、后端换 Token、查询用户、获取验签公钥的四个 Provider 端点。
                .authorizationUri(properties.getOidc().getAuthorizationUri())
                .tokenUri(properties.getOidc().getTokenUri())
                .userInfoUri(properties.getOidc().getUserInfoUri())
                // MaxKey 按 client_id 返回该应用生成的签名公钥。
                .jwkSetUri(properties.getOidc().getJwkSetUri() + "?client_id=" + maxKey.getClientId())
                // 校验 id_token 中的 iss，防止接受其他 Provider 签发的 Token。
                .issuerUri(properties.getOidc().getIssuer())
                // 使用 OIDC 标准 sub Claim 作为 Spring Security 用户的稳定唯一标识。
                .userNameAttributeName("sub")
                // 仅用于登录页或日志中的显示名称。
                .clientName("MaxKey")
                // 手工提供 Discovery 元数据，使 Spring 的 OIDC 组件仍能按标准字段读取端点。
                .providerConfigurationMetadata(Map.of(
                        "issuer", properties.getOidc().getIssuer(),
                        "authorization_endpoint", properties.getOidc().getAuthorizationUri(),
                        "token_endpoint", properties.getOidc().getTokenUri(),
                        "userinfo_endpoint", properties.getOidc().getUserInfoUri(),
                        "jwks_uri", properties.getOidc().getJwkSetUri() + "?client_id=" + maxKey.getClientId(),
                        "end_session_endpoint", properties.getFrontendUrl() + "/logout"))
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OidcDemoProperties properties) throws Exception {
        AuthenticationSuccessHandler successHandler = (request, response, authentication) ->
                // Spring 已验证 id_token、加载用户并建立本地 Session，此时返回 Vue 首页。
                response.sendRedirect(URI.create(properties.getFrontendUrl()).toString());

        http
                .authorizeHttpRequests(authorize -> authorize
                        // 健康检查和错误页无需登录。
                        .requestMatchers("/", "/error", "/api/public").permitAll()
                        // Vue 用该接口判断当前浏览器是否已有后端登录 Session。
                        .requestMatchers("/api/me").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        // 覆盖 Spring 默认 Token 解析器，以兼容 MaxKey 当前的 camelCase Token JSON。
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenResponseClient(new MaxKeyTokenResponseClient()))
                        .successHandler(successHandler))
                // 前端通过 fetch 查询登录状态时返回 401，避免被重定向到 OAuth 登录页造成跨域错误。
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .logout(logout -> logout
                        // Demo 为便于前端按钮调用，允许 GET /api/logout 清理本地 Session。
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "GET"))
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect(properties.getFrontendUrl())))
                // GET 注销没有 CSRF 请求体；这里只为 Demo 简化。生产项目更推荐 POST 注销并保留 CSRF 防护。
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/logout"));

        return http.build();
    }
}
