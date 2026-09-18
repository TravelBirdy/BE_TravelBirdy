package com.travelbird.ai.api;

/** Part3가 AI Preview 저장 경로를 생성·취소할 때 사용하는 Part2 계약. */
public interface AiPreviewSavedRouteService {
  void save(Long userId, Long previewId);
  void cancel(Long userId, Long previewId);
}
