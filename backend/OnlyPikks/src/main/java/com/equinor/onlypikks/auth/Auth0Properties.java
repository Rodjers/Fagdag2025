package com.equinor.onlypikks.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Properties(
        String domain,
        String audience,
        String clientId,
        String clientSecret,
        String connection,
        String defaultScope,
        Boolean mockTokensEnabled
) {
    public String issuerUri() {
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("auth0.domain must be configured");
        }
        return "https://" + domain + "/";
    }

    public String baseUrl() {
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("auth0.domain must be configured");
        }
        return "https://" + domain;
    }

    public String connectionOrDefault() {
        if (StringUtils.hasText(connection)) {
            return connection;
        }
        return "Username-Password-Authentication";
    }

    public String defaultScopeOrFallback() {
        if (StringUtils.hasText(defaultScope)) {
            return defaultScope;
        }
        return "openid profile email offline_access";
    }

    public boolean isMockTokensEnabled() {
        return Boolean.TRUE.equals(mockTokensEnabled);
    }
}
