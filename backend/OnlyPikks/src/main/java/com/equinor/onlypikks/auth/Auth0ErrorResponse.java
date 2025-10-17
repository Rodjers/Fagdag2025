package com.equinor.onlypikks.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Auth0ErrorResponse(
        String error,
        String errorDescription
) {
}
