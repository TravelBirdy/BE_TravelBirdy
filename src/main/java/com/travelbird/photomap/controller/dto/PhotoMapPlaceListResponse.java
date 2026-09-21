package com.travelbird.photomap.controller.dto;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.11.2. 필드명은 OpenAPI {@code PhotomapPlacesResponse.items}
 * (필수)를 따른다 — 이전엔 {@code places}로 나가서 프론트가 스펙대로 읽으면 항상 빈 배열로
 * 보이는 문제가 있었다(oriole0419 PR#16 리뷰).
 */
public record PhotoMapPlaceListResponse(
        List<PhotoMapPlaceItem> items
) {
}
