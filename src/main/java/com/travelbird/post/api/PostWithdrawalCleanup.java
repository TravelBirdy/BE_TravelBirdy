package com.travelbird.post.api;

/**
 * Part 3 → Part 1(회원탈퇴 Orchestrator) 공개 계약.
 *
 * <p>탈퇴 처리 2단계(콘텐츠 정리)에서 호출된다 — {@code posts.trip_id -> trips.trip_id}가
 * RESTRICT이므로 반드시 Part 2의 Trip 삭제보다 먼저 실행해야 한다.
 * (공통협의 4.3절 PostWithdrawalCleanup, 7절 회원탈퇴 실행순서)
 *
 * <p>역할: 작성 Post 삭제/tombstone 처리, 타인이 저장한 해당 Post의 SavedRoute를
 * {@code sourceAvailable=false} 로 변경, 본인의 {@code saved_places}/{@code saved_routes}/신고
 * 부가정보 정리.
 */
public interface PostWithdrawalCleanup {

	void cleanupUserContent(Long userId);
}
