package com.travelbird.savedroute.service;

import com.travelbird.ai.api.AiPreviewSavedRouteService;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 미리보기 경로 저장·취소. backend-functional-spec-v10.md §3.10.1 5단계 중
 * 1~4단계(소유권·만료 검증, retentionStatus 전환)는 Part2의 {@link AiPreviewSavedRouteService}가
 * 처리하고, 5단계(SavedRoute 생성/삭제)는 여기서 처리한다. {@code previewId} 하나당
 * 소유자는 항상 최대 1명이므로(저장 자체가 본인 소유 Preview만 가능), 유니크 제약이 곧
 * "활성 참조 0~1개"를 보장한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AiPreviewRouteSaveService {

    private final SavedRouteRepository savedRouteRepository;
    private final AiPreviewSavedRouteService aiPreviewSavedRouteService;

    public void save(Long userId, Long previewId) {
        aiPreviewSavedRouteService.save(userId, previewId); // 1~4단계 — 소유권/만료 검증, PERMANENT 전환

        savedRouteRepository.findByUserIdAndSourceTypeAndSourceId(userId, SavedRouteSourceType.AI_PREVIEW, previewId)
                .ifPresentOrElse(
                        existing -> existing.markAvailable(), // 멱등 처리
                        () -> savedRouteRepository.save(SavedRoute.of(userId, SavedRouteSourceType.AI_PREVIEW, previewId)));
    }

    /** 중복 취소는 멱등하게 처리한다 — 저장돼 있지 않으면 Part2 강등 호출도 하지 않는다. */
    public void cancel(Long userId, Long previewId) {
        long deleted = savedRouteRepository.deleteByUserIdAndSourceTypeAndSourceId(
                userId, SavedRouteSourceType.AI_PREVIEW, previewId);
        if (deleted > 0) {
            aiPreviewSavedRouteService.cancel(userId, previewId);
        }
    }
}
