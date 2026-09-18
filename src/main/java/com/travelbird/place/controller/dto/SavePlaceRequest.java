package com.travelbird.place.controller.dto;

/**
 * {@code PUT /api/users/me/saved-places/{placeId}} 요청. memo는 생략 가능(null 허용).
 */
public record SavePlaceRequest(
        String memo
) {
}
