package com.travelbird.ai.api;

/**
 * Part 3 SavedRoute use case가 AI Preview 보존 상태를 변경할 때 사용하는 Part 2 계약.
 * SavedRoute 생성·삭제와 마지막 활성 참조 판정은 Part 3의 책임이다.
 */
public interface AiPreviewSavedRouteService {
  /** Preview 소유권과 만료 여부를 검증하고 PERMANENT로 전환한다. */
  void save(Long userId, Long previewId);

  /** Part 3이 마지막 활성 SavedRoute를 삭제한 뒤 호출하여 TEMPORARY로 전환한다. */
  void cancel(Long userId, Long previewId);
}
