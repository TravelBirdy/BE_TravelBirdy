package com.travelbird.savedroute.controller.dto;

import java.util.List;

/** backend-functional-spec-v10.md §3.10.2 {@code SavedRouteListResponse}. */
public record SavedRouteListResponse(
        List<SavedRouteListItem> items,
        Long nextCursor
) {
}
