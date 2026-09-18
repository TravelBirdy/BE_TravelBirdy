package com.travelbird.place.controller.dto;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceStatus;

import java.math.BigDecimal;

/**
 * {@code GET /api/places/{placeId}} 응답. backend-functional-spec-v10.md §3.6.2 필드 그대로.
 * 폐업/비활성 장소도 오류가 아니라 {@code status=CLOSED}로 200 반환한다.
 */
public record PlaceDetailResponse(
        Long placeId,
        PlaceStatus status,
        String name,
        String externalCategory,
        PlaceCategory category,
        String address,
        RegionSummary region,
        BigDecimal latitude,
        BigDecimal longitude,
        String imageUrl,
        boolean saved
) {

    public static PlaceDetailResponse of(Place place, RegionSummary region, boolean saved) {
        return new PlaceDetailResponse(
                place.getPlaceId(),
                place.getStatus(),
                place.getName(),
                place.getExternalCategory(),
                place.getCategory(),
                place.getAddress(),
                region,
                place.getLatitude(),
                place.getLongitude(),
                place.getImageUrl(),
                saved
        );
    }
}
