package com.equinor.onlypikks.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PostSummaryResponse(
        String id,
        String title,
        String description,
        List<String> tags,
        PostVisibility visibility,
        String ownerId,
        String ownerDisplayName,
        String thumbnailUrl,
        Instant createdAt,
        Instant updatedAt,
        long commentCount,
        long likeCount
) {
}
