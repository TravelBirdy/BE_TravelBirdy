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

    /**
     * Part2의 {@code cancel()}(내부적으로 {@code AiPreviewService.makeTemporary}, 존재·소유·
     * 만료를 저장과 동일하게 검증)을 먼저 호출한 뒤 로컬 SavedRoute 행을 지운다 — 순서를
     * 반대로 하면(먼저 지우고 참조 없을 때만 호출) 없는/남의/만료된 previewId에 대한 취소가
     * 전부 조용히 204로 성공해버린다(oriole0419 PR#14 리뷰). {@code makeTemporary}는 이미
     * TEMPORARY면 아무것도 안 하는 멱등 연산이라, 실제로 저장한 적 없는(그러나 본인 소유·
     * 미만료인) previewId에 취소를 호출해도 부작용 없이 통과한다 — 검증 실패(존재하지 않음/
     * 남의 것/만료)만 예외로 이어진다.
     */
    public void cancel(Long userId, Long previewId) {
        aiPreviewSavedRouteService.cancel(userId, previewId);
        savedRouteRepository.deleteByUserIdAndSourceTypeAndSourceId(userId, SavedRouteSourceType.AI_PREVIEW, previewId);
    }
}
