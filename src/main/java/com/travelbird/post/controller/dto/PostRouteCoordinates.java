package com.travelbird.post.controller.dto;

import java.math.BigDecimal;

/**
 * backend-functional-spec-v10.md §3.8.3 {@code route}용 좌표. Part2의
 * {@code trip.dto.response.Coordinates}(내부 응답 타입)를 직접 참조하지 않고 Part3
 * 응답 전용으로 별도 정의한다(chun9930 PR#22 리뷰) — JSON 구조는 동일하게 유지한다.
 */
public record PostRouteCoordinates(BigDecimal latitude, BigDecimal longitude) {
}
