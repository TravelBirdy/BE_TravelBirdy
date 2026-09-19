package com.travelbird.place.service;

import com.travelbird.place.domain.PlaceExternalIdProvider;

/**
 * Import 후 AI 팀에 회신할 {@code provider/externalPlaceId/placeId} 매핑.
 * backend-functional-spec-v10.md §3.6.5 응답 예시 형식과 동일하다.
 */
public record PlaceExternalIdMapping(
        PlaceExternalIdProvider provider,
        String externalPlaceId,
        Long placeId
) {
}
