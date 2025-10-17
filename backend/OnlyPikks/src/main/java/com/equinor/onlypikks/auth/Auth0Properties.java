package com.equinor.onlypikks.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth0")
public record Auth0Properties(
        String domain,
        String audience,
        String clientId,
        String clientSecret,
        Boolean mockTokensEnabled
) {
    public String issuerUri() {
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("auth0.domain must be configured");
        }
        return "https://" + domain + "/";
    }

    public boolean isMockTokensEnabled() {
        return Boolean.TRUE.equals(mockTokensEnabled);
    }
}
