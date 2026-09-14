package com.travelbird.common.dto;

/**
 * 서비스 공통 지역 표시 DTO. 5자리 시군구 코드 기준.
 * 두 필드 모두 항상 존재하며 null을 허용하지 않는다. (backend-functional-spec-v10.md 문서 적용 원칙)
 *
 * <p>임시 위치: 파트 간 중복 정의 금지 대상이며 최종 소유 패키지는 팀 협의 필요
 * (공통협의 13절 — 공통 타입 목록에 있으나 소유 패키지는 미확정). 위치가 확정되면 그대로 이동한다.
 */
public record RegionSummary(
		String sigunguCode,
		String sigunguName
) {
}
