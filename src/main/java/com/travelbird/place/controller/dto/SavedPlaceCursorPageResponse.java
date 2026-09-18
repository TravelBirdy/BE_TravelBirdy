package com.travelbird.place.controller.dto;

import java.util.List;

/**
 * {@code GET /api/users/me/saved-places} 응답. OpenAPI 계약은 {@code items}와
 * required+nullable {@code nextCursor}만 정의한다 — {@code hasNext}는 없다
 * (다음 페이지가 있으면 {@code nextCursor}에 값, 없으면 {@code null}).
 */
public record SavedPlaceCursorPageResponse(
        List<SavedPlaceListItem> items,
        Long nextCursor
) {
}
