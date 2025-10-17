package com.equinor.onlypikks.api.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PagedResponse<T>(
        List<T> items,
        int page,
        int perPage,
        long total
) {
}
