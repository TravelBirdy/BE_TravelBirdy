package com.travelbird.place.api;

import java.util.List;

/**
 * Part 3 → Part 2(AI SAVED_PLACES 추천) 공개 계약.
 * 요청에 담긴 저장 장소 ID가 실제로 해당 사용자의 저장 장소인지 검증한다.
 */
public interface SavedPlaceReader {

	/**
	 * 전달된 모든 {@code placeId}가 해당 사용자의 저장 장소인지 검증한다.
	 */
	boolean areAllSavedByUser(Long userId, List<Long> placeIds);

	/**
	 * 사용자가 저장한 장소 ID 목록을 조회한다.
	 */
	List<Long> getSavedPlaceIds(Long userId);
}
