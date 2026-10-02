package com.travelbird.place.controller.dto;

import java.math.BigDecimal;

/**
 * backend-functional-spec-v10.md §3.6.4. 이 코드베이스는 Bean Validation을 요청 DTO에 걸지 않는
 * 관례라(예: {@code CreatePostRequest}) 필수 검증은 {@code PlaceResolveService}에서 한다.
 */
public record ResolvePlaceRequest(
        String externalPlaceId,
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
