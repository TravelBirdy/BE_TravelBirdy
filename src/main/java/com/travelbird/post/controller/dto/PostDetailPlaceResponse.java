package com.travelbird.post.controller.dto;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.enums.PlaceCategory;

import java.math.BigDecimal;
import java.util.List;

/**
 * backend-functional-spec-v10.md §3.8.3. {@code memo}는 {@code memoMasked=true}일 때
 * {@code null}이다 — 장소 사진({@code images})은 {@code MEMO_PRIVATE}에서도 항상 공개한다
 * (§3.8.2).
 */
public record PostDetailPlaceResponse(
        Long placeId,
        String name,
        PlaceCategory category,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        int dayNumber,
        int order,
        String memo,
        boolean memoMasked,
        List<ImageSummary> images
) {
}
