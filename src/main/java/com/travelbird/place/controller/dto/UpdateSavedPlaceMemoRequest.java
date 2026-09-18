package com.travelbird.place.controller.dto;

/**
 * {@code PATCH /api/users/me/saved-places/{placeId}/memo} 요청.
 */
public record UpdateSavedPlaceMemoRequest(
        String memo
) {
}
