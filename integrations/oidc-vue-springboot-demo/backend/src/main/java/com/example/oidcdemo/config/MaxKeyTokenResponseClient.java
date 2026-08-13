package com.example.oidcdemo.config;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 兼容 MaxKey Token 接口响应格式的授权码客户端。
 *
 * <p>Spring Security 默认只识别 access_token、token_type、expires_in 等标准字段。
 * MaxKey 当前还可能返回 value、tokenType、expiresIn，并把 id_token 放在
 * additionalInformation 中，因此需要在这里转换成 OAuth2AccessTokenResponse。</p>
 */
public class MaxKeyTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        // request 同时保存客户端注册信息，以及刚从 MaxKey 回调中取得的 authorization code。
        var registration = request.getClientRegistration();
        var authorizationResponse = request.getAuthorizationExchange().getAuthorizationResponse();

        // Authorization Code Flow 的服务端 Token 请求。secret 只在后端到 MaxKey 的请求中出现。
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        form.add(OAuth2ParameterNames.CODE, authorizationResponse.getCode());
        // 回调地址必须与 MaxKey 管理端登记值完全一致，否则 Token 端点会拒绝该授权码。
        form.add(OAuth2ParameterNames.REDIRECT_URI, registration.getRedirectUri());
        form.add(OAuth2ParameterNames.CLIENT_ID, registration.getClientId());
        form.add(OAuth2ParameterNames.CLIENT_SECRET, registration.getClientSecret());

        // Token Endpoint 使用 OAuth2 规定的表单编码，并期望 JSON 响应。
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        String body = restTemplate.postForObject(
                registration.getProviderDetails().getTokenUri(),
                new HttpEntity<>(form, headers),
                String.class);

        try {
            Map<String, Object> values = objectMapper.readValue(
                    body, new TypeReference<LinkedHashMap<String, Object>>() {});
            // MaxKey 可能用 HTTP 200 + OAuth2Exception 对象表达协议错误，需要主动识别。
            Map<String, Object> oauth2Exception = objectMap(values.get("OAuth2Exception"));
            if (oauth2Exception != null) {
                String code = stringValue(oauth2Exception.get("OAuth2ErrorCode"), "unknown_error");
                String message = stringValue(oauth2Exception.get("message"), "MaxKey Token request failed");
                throw new IllegalStateException("MaxKey Token request failed [" + code + "]: " + message);
            }

            // MaxKey 可能使用 camelCase 序列化 Token，并将 id_token 等 OIDC 扩展字段
            // 放入 additionalInformation，因此先将嵌套字段展开。
            Map<String, Object> additionalInformation = objectMap(values.get("additionalInformation"));
            if (additionalInformation != null) {
                values.putAll(additionalInformation);
            }

            // 同时接受标准 access_token 和 MaxKey 的 value 字段。
            String accessToken = requiredString(values, OAuth2ParameterNames.ACCESS_TOKEN, "value");
            OAuth2AccessTokenResponse.Builder response = OAuth2AccessTokenResponse
                    .withToken(accessToken)
                    .tokenType(OAuth2AccessToken.TokenType.BEARER);

            Object expiresIn = firstValue(values, OAuth2ParameterNames.EXPIRES_IN, "expiresIn");
            if (expiresIn != null) {
                response.expiresIn(Long.parseLong(String.valueOf(expiresIn)));
            }
            Object refreshToken = firstValue(values, OAuth2ParameterNames.REFRESH_TOKEN, "refreshToken");
            if (refreshToken != null) {
                response.refreshToken(String.valueOf(refreshToken));
            }
            if (values.get(OAuth2ParameterNames.SCOPE) != null) {
                response.scopes(scopeValues(values.get(OAuth2ParameterNames.SCOPE)));
            }

            // 移除已映射字段，剩余 id_token 等扩展参数交给 Spring 的 OIDC 登录流程。
            values.remove(OAuth2ParameterNames.ACCESS_TOKEN);
            values.remove(OAuth2ParameterNames.TOKEN_TYPE);
            values.remove(OAuth2ParameterNames.EXPIRES_IN);
            values.remove(OAuth2ParameterNames.REFRESH_TOKEN);
            values.remove(OAuth2ParameterNames.SCOPE);
            values.remove("value");
            values.remove("tokenType");
            values.remove("expiresIn");
            values.remove("refreshToken");
            values.remove("additionalInformation");
            values.remove("OAuth2Exception");
            values.remove("expiration");
            values.remove("expired");
            response.additionalParameters(values);
            return response.build();
        } catch (Exception ex) {
            throw new IllegalStateException("MaxKey Token response parsing failed: " + body, ex);
        }
    }

    private static String requiredString(Map<String, Object> values, String... names) {
        String value = stringValue(firstValue(values, names), null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Token response misses field: " + String.join(" or ", names));
        }
        return value;
    }

    private static Object firstValue(Map<String, Object> values, String... names) {
        for (String name : names) {
            if (values.get(name) != null) {
                return values.get(name);
            }
        }
        return null;
    }

    private static String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : null;
    }

    private static java.util.Set<String> scopeValues(Object value) {
        java.util.Set<String> scopes = new java.util.LinkedHashSet<>();
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> scopes.add(String.valueOf(item)));
        } else {
            String text = String.valueOf(value);
            for (String scope : text.split("[ ,]+")) {
                if (!scope.isBlank()) {
                    scopes.add(scope);
                }
            }
        }
        return scopes;
    }
}
