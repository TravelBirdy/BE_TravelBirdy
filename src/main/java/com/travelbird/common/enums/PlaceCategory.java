package com.travelbird.common.enums;

/**
 * 장소 대분류. MVP 고정값 9개 — 앱 전용 커스텀 카테고리를 임의로 추가하지 않는다.
 * (backend-functional-spec-v10.md 3.6.1)
 *
 * <p>임시 위치: 파트 간 중복 정의 금지 대상이며 최종 소유 패키지는 팀 협의 필요
 * (공통협의 13절 — 공통 타입 목록에 있으나 소유 패키지는 미확정).
 */
public enum PlaceCategory {
	FOOD,
	CAFE,
	ATTRACTION,
	CULTURE_ART,
	ACTIVITY,
	NATURE,
	SHOPPING,
	ACCOMMODATION,
	OTHER
}
