package com.travelbird.post.controller.dto;

import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;

import java.time.LocalDateTime;

/**
 * 작성(POST)·수정(PATCH) 공통 응답. backend-functional-spec-v10.md §3.8.1. {@code version}은
 * PATCH 요청에 필요한 낙관적 락 값으로, 생성 시점 값을 그대로 내려준다(팀장 확인, PATCH
 * 구현 전 스펙 질의 답변).
 */
public record PostMutationResponse(
        Long postId,
        PostStatus status,
        PostVisibility visibility,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        boolean tripRouteLocked,
        Long version
) {
}
