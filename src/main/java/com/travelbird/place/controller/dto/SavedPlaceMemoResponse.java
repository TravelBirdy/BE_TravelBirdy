package com.travelbird.place.controller.dto;

import java.time.LocalDateTime;

/**
 * {@code PATCH /api/users/me/saved-places/{placeId}/memo} 응답. OpenAPI 계약대로
 * {@code 200 OK} + {@code placeId}/{@code memo}/{@code updatedAt}을 반환한다.
 */
public record SavedPlaceMemoResponse(
        Long placeId,
        String memo,
        LocalDateTime updatedAt
) {
}
