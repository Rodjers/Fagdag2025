package com.equinor.onlypikks.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MockAuthService {

    public Optional<AuthContext> resolve(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }
        String userId = token;
        String email = "%s@example.com".formatted(token);
        String displayName = token.replace('-', ' ');
        return Optional.of(new AuthContext(userId, email, displayName));
    }
}
