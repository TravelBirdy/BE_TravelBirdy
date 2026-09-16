package com.travelbird.place.api;

import java.util.List;

/**
 * Part 3 → Part 1(Home) · Part 2(Trip/AI) 공개 계약.
 * canonical {@code placeId}는 {@code places.place_id} 하나뿐이며, TourAPI 초기 Import로만 생성된다
 * (2026-09 NAVER Search API 이용약관 개정 반영 — 팀 확인 필요, 공통협의 9절 Place 경계).
 *
 * <p>다른 파트는 이 인터페이스로만 장소 정보를 조회하며, {@code places} Repository를
 * 직접 참조하지 않는다.
 */
public interface PlaceReader {

	/**
	 * 장소 존재 여부를 검증한다.
	 */
	boolean existsPlace(Long placeId);

	/**
	 * 단일 장소 정보를 조회한다. 존재하지 않으면 예외를 던진다.
	 *
	 * @param viewerIdOrNull {@link PlaceContract#saved()} 계산 기준. 비로그인/viewer 미상이면
	 *                       {@code null}이고 이 경우 {@code saved}는 항상 {@code false}다.
	 */
	PlaceContract getPlace(Long placeId, Long viewerIdOrNull);

	/**
	 * 여러 장소 정보를 한 번에 조회한다. 존재하지 않는 ID는 결과에서 제외한다.
	 *
	 * @param viewerIdOrNull {@link #getPlace(Long, Long)} 와 동일.
	 */
	List<PlaceContract> getPlaces(List<Long> placeIds, Long viewerIdOrNull);
}
