package com.travelbird.post.controller.dto;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.8.3. Day별 장소를 방문 순서대로 좌표만 뽑은 지도
 * 표시용 데이터(네이버 Directions 결과 아님, 지도·경로 표시 정책과 동일).
 */
public record PostDetailRouteDay(
        int dayNumber,
        List<PostRouteCoordinates> routePoints
) {
}
