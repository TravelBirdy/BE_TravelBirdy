package com.travelbird.place.api;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.PlaceStatus;

import java.math.BigDecimal;

/**
 * 파트 경계를 넘어 반환하는 불변 장소 계약. Entity를 직접 반환하지 않는다.
 * (공통협의 3.2절 — Entity를 파트 경계 밖으로 반환하지 않음)
 */
public record PlaceContract(
		Long placeId,
		String sigunguCode,
		PlaceCategory category,
		BigDecimal latitude,
		BigDecimal longitude,
		PlaceStatus status
) {
}
