package com.travelbird.place.api;

import com.travelbird.common.enums.PlaceCategory;

import java.math.BigDecimal;

/**
 * @deprecated {@link AiPlaceSyncReader} 참고 — 2026-09 NAVER 정책 변경으로 이 계약의
 * 원래 목적(NAVER-only 신규 장소 판정)이 사실상 소멸했다. Part 2와 존속 여부 재확인 전까지
 * 신규로 의존하지 않는다.
 */
@Deprecated
public record AiPlaceSyncContract(
		Long placeId,
		String sigunguCode,
		String name,
		String address,
		BigDecimal latitude,
		BigDecimal longitude,
		PlaceCategory category,
		boolean tourApiMapped
) {
}
