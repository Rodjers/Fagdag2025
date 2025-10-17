package com.equinor.onlypikks.config;

import com.equinor.onlypikks.auth.Auth0AudienceValidator;
import com.equinor.onlypikks.auth.Auth0Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(Auth0Properties.class)
public class Auth0Config {

    @Bean
    public JwtDecoder jwtDecoder(Auth0Properties properties) {
        if (properties.isMockTokensEnabled()) {
            return token -> {
                String subject = StringUtils.hasText(token) ? token : "anonymous";
                Instant now = Instant.now();
                Map<String, Object> headers = Map.of("alg", "none");
                Map<String, Object> claims = Map.of(
                        "sub", subject,
                        "email", subject + "@example.com",
                        "name", subject
                );
                return new Jwt(token, now, now.plusSeconds(3600), headers, claims);
            };
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(properties.issuerUri()).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(properties.issuerUri());
        OAuth2TokenValidator<Jwt> withAudience = new Auth0AudienceValidator(properties.audience());
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));

        return decoder;
    }
}
