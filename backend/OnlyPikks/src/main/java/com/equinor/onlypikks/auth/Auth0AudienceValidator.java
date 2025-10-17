package com.equinor.onlypikks.auth;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import java.util.List;

public class Auth0AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final String ERROR_CODE_INVALID_AUDIENCE = "invalid_token";

    private final String audience;

    public Auth0AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (!StringUtils.hasText(audience)) {
            return OAuth2TokenValidatorResult.success();
        }

        List<String> audiences = token.getAudience();
        if (audiences != null && audiences.contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(ERROR_CODE_INVALID_AUDIENCE, "The required audience is missing", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
