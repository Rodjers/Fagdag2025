package com.equinor.onlypikks.auth;

import com.equinor.onlypikks.api.model.AuthTokensResponse;
import com.equinor.onlypikks.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class Auth0AuthenticationClient {

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);

    private final Auth0Properties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public Auth0AuthenticationClient(
            Auth0Properties properties,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = objectMapper;
    }

    public AuthTokensResponse login(String email, String password) {
        if (properties.isMockTokensEnabled()) {
            return buildMockTokenResponse();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("grant_type", "password");
        payload.put("username", email);
        payload.put("password", password);
        payload.put("audience", properties.audience());
        payload.put("client_id", properties.clientId());
        payload.put("client_secret", properties.clientSecret());
        payload.put("scope", properties.defaultScopeOrFallback());
        String realm = properties.connectionOrDefault();
        if (StringUtils.hasText(realm)) {
            payload.put("realm", realm);
        }

        return exchangeForTokens(payload, "Invalid credentials");
    }

    public AuthTokensResponse refresh(String refreshToken) {
        if (properties.isMockTokensEnabled()) {
            return buildMockTokenResponse();
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("grant_type", "refresh_token");
        payload.put("refresh_token", refreshToken);
        payload.put("client_id", properties.clientId());
        payload.put("client_secret", properties.clientSecret());
        payload.put("scope", properties.defaultScopeOrFallback());

        return exchangeForTokens(payload, "Invalid or expired refresh token");
    }

    public void logout(String refreshToken) {
        if (properties.isMockTokensEnabled()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("token", refreshToken);
        body.add("token_type_hint", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(properties.baseUrl() + "/oauth/revoke", entity, Void.class);
        } catch (HttpStatusCodeException ex) {
            throw new UnauthorizedException(resolveErrorMessage("Unable to logout", ex));
        } catch (RestClientException ex) {
            throw new UnauthorizedException(buildConnectivityError("Unable to reach authentication service", ex));
        }
    }

    public void register(String email, String password) {
        if (properties.isMockTokensEnabled()) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("client_id", properties.clientId());
        payload.put("email", email);
        payload.put("password", password);
        payload.put("connection", properties.connectionOrDefault());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForEntity(properties.baseUrl() + "/dbconnections/signup", entity, Map.class);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException(resolveErrorMessage("Unable to register user", ex));
            }
            throw new IllegalStateException(resolveErrorMessage("Unable to register user", ex));
        } catch (RestClientException ex) {
            throw new IllegalStateException(buildConnectivityError("Unable to reach authentication service", ex));
        }
    }

    private AuthTokensResponse exchangeForTokens(Map<String, Object> payload, String fallbackError) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Auth0TokenResponse> response = restTemplate.postForEntity(
                    properties.baseUrl() + "/oauth/token",
                    entity,
                    Auth0TokenResponse.class
            );

            Auth0TokenResponse body = response.getBody();
            if (body == null || !StringUtils.hasText(body.accessToken())) {
                throw new UnauthorizedException("Authentication provider returned an empty response");
            }

            String tokenType = StringUtils.hasText(body.tokenType()) ? body.tokenType() : "Bearer";
            long expiresIn = body.expiresIn() > 0 ? body.expiresIn() : ACCESS_TOKEN_TTL.toSeconds();

            return new AuthTokensResponse(body.accessToken(), body.refreshToken(), tokenType, expiresIn);
        } catch (HttpStatusCodeException ex) {
            throw new UnauthorizedException(resolveErrorMessage(fallbackError, ex));
        } catch (RestClientException ex) {
            throw new UnauthorizedException(buildConnectivityError("Unable to reach authentication service", ex));
        }
    }

    private String buildConnectivityError(String fallback, RestClientException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause != null && StringUtils.hasText(cause.getMessage())) {
            return fallback + ": " + cause.getMessage();
        }
        if (StringUtils.hasText(ex.getMessage())) {
            return fallback + ": " + ex.getMessage();
        }
        return fallback;
    }

    private AuthTokensResponse buildMockTokenResponse() {
        String accessToken = "access-" + UUID.randomUUID();
        String refreshToken = "refresh-" + UUID.randomUUID();
        return new AuthTokensResponse(accessToken, refreshToken, "Bearer", ACCESS_TOKEN_TTL.toSeconds());
    }

    private String resolveErrorMessage(String fallback, HttpStatusCodeException ex) {
        String responseBody = ex.getResponseBodyAsString();
        if (StringUtils.hasText(responseBody)) {
            try {
                Auth0ErrorResponse error = objectMapper.readValue(responseBody, Auth0ErrorResponse.class);
                if (error != null) {
                    if (StringUtils.hasText(error.errorDescription())) {
                        return error.errorDescription();
                    }
                    if (StringUtils.hasText(error.error())) {
                        return error.error();
                    }
                }
            } catch (JsonProcessingException ignored) {
                // Ignore parsing errors and fall back to default message
            }
        }
        return fallback;
    }
}
