package com.travelbird.post.controller.dto;

import com.travelbird.trip.dto.response.Coordinates;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.8.3. Day별 장소를 방문 순서대로 좌표만 뽑은 지도
 * 표시용 데이터(네이버 Directions 결과 아님, 지도·경로 표시 정책과 동일).
 *
 * <p>{@code Coordinates}는 새로 만들지 않고 Part2의 {@code trip.dto.response.Coordinates}를
 * 그대로 재사용한다 — 공통협의 "타입 중복 정의 금지" 규칙에 따른 판단이지만, Part3가
 * Part2의 dto 패키지를 직접 참조하는 게 맞는 경계인지는 PR 리뷰 요청에 명시한다.
 */
public record PostDetailRouteDay(
        int dayNumber,
        List<Coordinates> routePoints
) {
}
