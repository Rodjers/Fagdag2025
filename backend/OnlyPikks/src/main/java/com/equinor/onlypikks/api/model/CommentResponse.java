package com.equinor.onlypikks.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CommentResponse(
        String id,
        String postId,
        String authorId,
        String authorDisplayName,
        String text,
        Instant createdAt,
        Instant updatedAt
) {
}
