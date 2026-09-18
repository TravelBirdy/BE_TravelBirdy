package com.travelbird.place.controller.dto;

import java.util.List;

public record SavedPlaceCursorPageResponse(
        List<SavedPlaceListItem> items,
        Long nextCursor,
        boolean hasNext
) {
}
