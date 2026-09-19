package com.travelbird.home.dto.response;

import com.travelbird.common.enums.PlaceCategory;
import java.math.BigDecimal;

public record PlaceSummary(
        Long placeId,
        String externalPlaceId,
        String name,
        String externalCategory,
        PlaceCategory category,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean saved
) {
}
