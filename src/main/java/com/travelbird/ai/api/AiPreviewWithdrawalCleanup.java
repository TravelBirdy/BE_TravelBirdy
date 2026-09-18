package com.travelbird.ai.api;

import java.util.Collection;

/**
 * 회원 탈퇴 과정에서 삭제된 AI Preview 저장 경로의 보관 상태를 정리하는 Part2 계약.
 *
 * <p>호출자는 SavedRoute 삭제 후 영향받은 preview ID를 전달한다. 구현체는 활성 참조가
 * 남아 있지 않은 Preview만 TEMPORARY 상태로 강등하며 SavedRoute 자체는 삭제하지 않는다.
 */
public interface AiPreviewWithdrawalCleanup {

  void demoteUnreferencedPreviews(Collection<Long> previewIds);
}
