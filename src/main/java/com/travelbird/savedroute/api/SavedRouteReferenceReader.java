package com.travelbird.savedroute.api;

/**
 * Part 3 → Part 2(AI Preview 만료·강등 안전 검증) 공개 계약.
 * Part 2의 만료 처리는 {@code retentionStatus=TEMPORARY}를 1차 기준으로 사용하고,
 * 필요 시 이 계약으로 SavedRoute 활성 참조가 없는지 추가 확인한다.
 * (공통협의 4.3절 SavedRouteReferenceReader)
 */
public interface SavedRouteReferenceReader {

	boolean existsAvailableAiPreviewReference(Long previewId);
}
