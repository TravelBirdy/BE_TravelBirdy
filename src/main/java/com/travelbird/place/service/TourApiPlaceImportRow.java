package com.travelbird.place.service;

import java.math.BigDecimal;

/**
 * TourAPI Import 1건. backend-functional-spec-v10.md §3.6.5 최소 필드 그대로
 * — {@code externalPlaceId}는 TourAPI {@code contentId}, {@code regionCode}는 5자리
 * 시군구 코드다. AI 쪽이 넘겨주는 원본 카테고리는 {@code rawCategory}에 그대로 담아
 * {@code PlaceCategoryMapper}로 분류한다.
 */
public record TourApiPlaceImportRow(
        String externalPlaceId,
        String name,
        String address,
        String rawCategory,
        BigDecimal latitude,
        BigDecimal longitude,
        String regionCode,
        String description,
        String imageUrl
) {
}
