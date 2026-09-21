package com.travelbird.photomap.controller.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * backend-functional-spec-v10.md §3.11.2. {@code postIds}는 스펙 원문 "같은 장소의
 * 다른 Post ID 목록"을 그대로 따라 {@code representativePostId}를 제외한 나머지를
 * 최신순으로 담는다(판단, PR 리뷰 요청 — "다른"이 대표를 뺀 목록이 아니라 전체 목록을
 * 가리키는 것일 수도 있어서 리뷰에서 재확인 필요).
 */
public record PhotoMapPlaceItem(
        Long placeId,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        long visitCount,
        Long representativePostId,
        List<Long> postIds
) {
}
