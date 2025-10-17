package com.equinor.onlypikks.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;

    public AuthService(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public Optional<AuthContext> resolve(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            String userId = jwt.getSubject();
            if (!StringUtils.hasText(userId)) {
                return Optional.empty();
            }

            String email = firstNonEmpty(
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("upn"),
                    userId
            );

            String displayName = firstNonEmpty(
                    jwt.getClaimAsString("name"),
                    jwt.getClaimAsString("nickname"),
                    jwt.getClaimAsString("preferred_username"),
                    email,
                    userId
            );

            return Optional.of(new AuthContext(userId, email, displayName));
        } catch (JwtException ex) {
            return Optional.empty();
        }
    }

    private String firstNonEmpty(String... values) {
        return Arrays.stream(values)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("");
    }
}
