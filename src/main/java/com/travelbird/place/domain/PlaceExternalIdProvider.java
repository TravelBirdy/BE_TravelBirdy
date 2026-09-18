package com.travelbird.place.domain;

/**
 * {@code place_external_ids.provider} 값. TourAPI로 최초 수집된 canonical 장소는
 * {@code KTO_TOUR_API}, NAVER는 검색 결과 표시 전용이라 현재는 신규 canonical 생성에
 * 쓰이지 않는다 (2026-09 NAVER Search API 이용약관 개정, place.api.PlaceReader 참고).
 */
public enum PlaceExternalIdProvider {
    KTO_TOUR_API,
    NAVER
}
