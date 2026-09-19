package com.travelbird.place.controller.dto;

import com.travelbird.common.enums.PlaceCategory;

import java.time.LocalDateTime;

/**
 * {@code GET /api/users/me/saved-places} 목록 항목. backend-functional-spec-v10.md §3.6.3.
 */
public record SavedPlaceListItem(
        Long placeId,
        String name,
        PlaceCategory category,
        String thumbnailUrl,
        String memo,
        LocalDateTime savedAt
) {
}
