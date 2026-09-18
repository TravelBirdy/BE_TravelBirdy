package com.travelbird.place.api;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.PlaceStatus;

import java.math.BigDecimal;

/**
 * 파트 경계를 넘어 반환하는 불변 장소 계약. Entity를 직접 반환하지 않는다.
 * (공통협의 3.2절 — Entity를 파트 경계 밖으로 반환하지 않음)
 *
 * <p>{@code name}/{@code address}는 공통협의 4.3절 최소 필드, OpenAPI {@code PlaceSummary},
 * dbml {@code places} 테이블 모두에서 NOT NULL이라 포함한다. {@code saved}는 조회 시점의
 * viewer 기준 저장 여부이며, 비로그인/viewer 미상이면 항상 {@code false}다.
 */
public record PlaceContract(
		Long placeId,
		String name,
		String address,
		String sigunguCode,
		PlaceCategory category,
		BigDecimal latitude,
		BigDecimal longitude,
		PlaceStatus status,
		boolean saved
) {
}
