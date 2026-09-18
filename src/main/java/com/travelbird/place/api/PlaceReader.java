package com.travelbird.place.api;

import java.util.List;

/**
 * Part 3 → Part 1(Home) · Part 2(Trip/AI) 공개 계약.
 * canonical {@code placeId}는 {@code places.place_id} 하나뿐이며, TourAPI 초기 Import로만 생성된다
 * (2026-09 NAVER Search API 이용약관 개정 반영, 공통협의 9절 Place 경계). 모든 canonical 장소가
 * TourAPI 출처이므로 별도 "AI sync 대상 여부" 판정이 필요 없어졌고, Part 2는 이 인터페이스만으로
 * AI 추천 런타임 sync 입력(canonical 장소 데이터)을 조립한다. {@code sigunguCode}를 AI 쪽
 * {@code regionCode}로 매핑해서 쓰면 되고, {@link PlaceContract#saved()}·{@link PlaceContract#status()}는
 * sync payload에 포함하지 않는다 (기존 {@code AiPlaceSyncReader}/{@code AiPlaceSyncContract}는
 * 이 계약으로 대체되어 제거함).
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
