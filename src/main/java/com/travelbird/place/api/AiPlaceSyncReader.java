package com.travelbird.place.api;

import java.util.Optional;

/**
 * Part 3 → Part 2(AI Recommendation 런타임 Place Sync) 공개 계약.
 *
 * @deprecated 2026-09 NAVER Search API 이용약관 개정으로 NAVER 검색 결과를 canonical Place로
 * 승격하는 흐름이 MVP에서 제거되었다. 이 계약이 담당하던 "NAVER-only 신규 장소 판정"
 * 시나리오가 사실상 소멸했으므로, Part 2와 존속 여부·대체 설계를 재확인하기 전까지
 * 구현·소비 모두 보류한다. (NAVER Search API 이용약관 개정 관련 팀 확인 필요,
 * 공통협의 9절 Place 경계)
 */
@Deprecated
public interface AiPlaceSyncReader {

	Optional<AiPlaceSyncContract> findForSync(Long placeId);
}
