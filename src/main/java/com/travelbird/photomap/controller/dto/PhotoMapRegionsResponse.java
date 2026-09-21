package com.travelbird.photomap.controller.dto;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.11.1. {@code recordCount}는 같은 장소가 여러
 * 게시글에 걸려도 dedup하지 않은 원시 (post, place) 레코드 수다 — {@code visitedPlaceCount}만
 * {@code COUNT(DISTINCT placeId)}. 지역 정렬은 스펙에 명시가 없어 sigunguCode 오름차순으로
 * 뒀다(판단, PR 리뷰 요청).
 */
public record PhotoMapRegionsResponse(
        List<PhotoMapRegionItem> regions,
        long totalVisitedRegionCount
) {
}
