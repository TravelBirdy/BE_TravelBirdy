package com.travelbird.savedroute.controller.dto;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.common.enums.TravelTheme;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.savedroute.domain.SavedRouteSourceType;

import java.time.LocalDateTime;
import java.util.List;

/** backend-functional-spec-v10.md §3.10.2 {@code SavedRouteListItem}. */
public record SavedRouteListItem(
        Long savedRouteId,
        SavedRouteSourceType sourceType,
        Long sourceId,
        boolean sourceAvailable,
        String title,
        String thumbnailUrl,
        AuthorSummary author,
        RegionSummary region,
        List<String> placeNames,
        List<TravelTheme> themes,
        LocalDateTime savedAt,
        LocalDateTime updatedAt,
        boolean editable
) {
}
