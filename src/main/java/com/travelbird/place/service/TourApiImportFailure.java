package com.travelbird.place.service;

/**
 * TourAPI Import 1건 실패. {@code reason}은 카테고리 제외 규칙 매치 또는 예외 메시지다.
 */
public record TourApiImportFailure(String externalPlaceId, String reason) {
}
