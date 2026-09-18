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
 *
 * <p><b>AI_PREVIEW 강등 호출 경계 (Part 2와 확정, 2026-09-18)</b><br>
 * 본인의 {@code saved_routes} 중 {@code sourceType=AI_PREVIEW} 인 것은 Part 3가 직접 삭제하고,
 * 영향받은 {@code previewId} 목록을 Part 2의
 * {@code com.travelbird.ai.api.AiPreviewWithdrawalCleanup#demoteUnreferencedPreviews(Collection<Long> previewIds)}
 * 에 전달한다. <b>Part 3는 대화형 저장 취소 흐름의 {@code AiPreviewSavedRouteService.cancel()} 을
 * 재사용하지 않는다</b> — 그 메서드는 SavedRoute 삭제까지 스스로 수행하므로, Part 3가 이미 삭제한
 * 뒤 그대로 호출하면 중복 삭제가 된다. {@code demoteUnreferencedPreviews}는 전달받은 Preview의
 * 활성 참조 여부만 확인해서 없으면 {@code PERMANENT -> TEMPORARY} 로 강등하고 SavedRoute는
 * 삭제하지 않는다. 그렇지 않으면 아무도 참조하지 않는 Preview가 {@code PERMANENT}로 고아 상태로
 * 남는다. (PR #4 chun9930 리뷰 코멘트 · 커밋 77c00aa, 공통협의 8절/9절 AI Preview SavedRoute 저장
 * 취소 절차 참고 — 단, 탈퇴 흐름은 삭제/강등 책임이 Part 3/Part 2로 분리된다는 점이 다름)
 */
public interface PostWithdrawalCleanup {

	void cleanupUserContent(Long userId);
}
