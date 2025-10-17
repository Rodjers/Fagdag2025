package com.equinor.onlypikks.auth;

public record AuthContext(
        String userId,
        String email,
        String displayName
) {
}
