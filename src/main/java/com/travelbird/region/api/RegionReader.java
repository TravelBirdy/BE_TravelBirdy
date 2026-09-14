package com.travelbird.region.api;

import com.travelbird.common.dto.RegionSummary;

/**
 * Part 3 → Part 1(Event/Home) · Part 2(Trip/AI) 공개 계약.
 * {@code sigungu_master} Owner = Part 3.
 *
 * <p>다른 파트는 이 인터페이스로만 지역 정보를 조회하며, {@code sigungu_master} Repository를
 * 직접 참조하지 않는다. (공통협의 4.3절 RegionReader)
 */
public interface RegionReader {

	/**
	 * 5자리 시군구 코드 존재 여부를 검증한다.
	 */
	boolean existsBySigunguCode(String sigunguCode);

	/**
	 * 시군구 코드로 지역 요약 정보를 조회한다. 존재하지 않으면 예외를 던진다.
	 */
	RegionSummary getRegion(String sigunguCode);
}
