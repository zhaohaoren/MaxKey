package com.example.oidcdemo.config;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
@EnableConfigurationProperties(OidcDemoProperties.class)
public class SecurityConfiguration {

    @Value("${spring.security.oauth2.client.registration.maxkey.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.maxkey.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.maxkey.redirect-uri}")
    private String redirectUri;

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(OidcDemoProperties properties) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("maxkey")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("openid", "profile", "email")
                .authorizationUri(properties.getOidc().getAuthorizationUri())
                .tokenUri(properties.getOidc().getTokenUri())
                .userInfoUri(properties.getOidc().getUserInfoUri())
                .jwkSetUri(properties.getOidc().getJwkSetUri() + "?client_id=" + clientId)
                .issuerUri(properties.getOidc().getIssuer())
                .userNameAttributeName("sub")
                .clientName("MaxKey")
                .providerConfigurationMetadata(Map.of(
                        "issuer", properties.getOidc().getIssuer(),
                        "authorization_endpoint", properties.getOidc().getAuthorizationUri(),
                        "token_endpoint", properties.getOidc().getTokenUri(),
                        "userinfo_endpoint", properties.getOidc().getUserInfoUri(),
                        "jwks_uri", properties.getOidc().getJwkSetUri() + "?client_id=" + clientId,
                        "end_session_endpoint", properties.getFrontendUrl() + "/logout"))
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            OidcDemoProperties properties) throws Exception {
        AuthenticationSuccessHandler successHandler = (request, response, authentication) ->
                response.sendRedirect(URI.create(properties.getFrontendUrl()).toString());

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/error", "/api/public").permitAll()
                        .requestMatchers("/api/me").authenticated()
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                .accessTokenResponseClient(new MaxKeyTokenResponseClient()))
                        .successHandler(successHandler))
                // 前端通过 fetch 查询登录状态时返回 401，避免被重定向到 OAuth 登录页造成跨域错误。
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "GET"))
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.sendRedirect(properties.getFrontendUrl())))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/logout"));

        return http.build();
    }
}
