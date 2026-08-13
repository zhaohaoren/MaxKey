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

/** 兼容 MaxKey Token 接口响应格式的授权码客户端。 */
public class MaxKeyTokenResponseClient
        implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest request) {
        var registration = request.getClientRegistration();
        var authorizationResponse = request.getAuthorizationExchange().getAuthorizationResponse();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(OAuth2ParameterNames.GRANT_TYPE, "authorization_code");
        form.add(OAuth2ParameterNames.CODE, authorizationResponse.getCode());
        // MaxKey requires the callback URL to exactly match the registered value.
        form.add(OAuth2ParameterNames.REDIRECT_URI, registration.getRedirectUri());
        form.add(OAuth2ParameterNames.CLIENT_ID, registration.getClientId());
        form.add(OAuth2ParameterNames.CLIENT_SECRET, registration.getClientSecret());

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
            Map<String, Object> oauth2Exception = objectMap(values.get("OAuth2Exception"));
            if (oauth2Exception != null) {
                String code = stringValue(oauth2Exception.get("OAuth2ErrorCode"), "unknown_error");
                String message = stringValue(oauth2Exception.get("message"), "MaxKey Token request failed");
                throw new IllegalStateException("MaxKey Token request failed [" + code + "]: " + message);
            }

            String accessToken = requiredString(values, OAuth2ParameterNames.ACCESS_TOKEN);
            OAuth2AccessTokenResponse.Builder response = OAuth2AccessTokenResponse
                    .withToken(accessToken)
                    .tokenType(OAuth2AccessToken.TokenType.BEARER);

            Object expiresIn = values.get(OAuth2ParameterNames.EXPIRES_IN);
            if (expiresIn != null) {
                response.expiresIn(Long.parseLong(String.valueOf(expiresIn)));
            }
            if (values.get(OAuth2ParameterNames.REFRESH_TOKEN) != null) {
                response.refreshToken(String.valueOf(values.get(OAuth2ParameterNames.REFRESH_TOKEN)));
            }
            if (values.get(OAuth2ParameterNames.SCOPE) != null) {
                response.scopes(scopeValues(values.get(OAuth2ParameterNames.SCOPE)));
            }

            values.remove(OAuth2ParameterNames.ACCESS_TOKEN);
            values.remove(OAuth2ParameterNames.TOKEN_TYPE);
            values.remove(OAuth2ParameterNames.EXPIRES_IN);
            values.remove(OAuth2ParameterNames.REFRESH_TOKEN);
            values.remove(OAuth2ParameterNames.SCOPE);
            response.additionalParameters(values);
            return response.build();
        } catch (Exception ex) {
            throw new IllegalStateException("MaxKey Token response parsing failed: " + body, ex);
        }
    }

    private static String requiredString(Map<String, Object> values, String name) {
        String value = stringValue(values.get(name), null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Token response misses field: " + name);
        }
        return value;
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
