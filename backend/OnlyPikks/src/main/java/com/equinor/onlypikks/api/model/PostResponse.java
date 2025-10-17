package com.equinor.onlypikks.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PostResponse(
        String id,
        String title,
        String description,
        List<String> tags,
        PostVisibility visibility,
        String ownerId,
        String ownerDisplayName,
        String fileId,
        String fileUrl,
        String thumbnailUrl,
        String originalFileName,
        long fileSize,
        Instant createdAt,
        Instant updatedAt,
        long commentCount,
        long likeCount,
        List<CommentResponse> latestComments
) {
}
