package com.travelbird.post.controller.dto;

import com.travelbird.post.domain.PostVisibility;

import java.util.List;

/**
 * backend-functional-spec-v10.md §3.8.1. {@code title}/{@code content}는 DRAFT
 * 저장에서 비어 있거나 {@code null}이어도 되므로 필수 검증하지 않는다 —
 * {@code publish=true}일 때만 {@code PostContentValidator.validateRequiredForPublish}로
 * 검증한다. 이 코드베이스는 Bean Validation을 요청 DTO에 걸지 않는 관례라(예:
 * {@code CreateTripRequest}), {@code tripId}/{@code visibility}/{@code publish}
 * 필수 검증도 {@code PostCreateService}에서 {@code VALIDATION_ERROR}로 처리한다.
 */
public record CreatePostRequest(
        Long tripId,
        String title,
        String content,
        Long representativeFileId,
        List<Long> imageFileIds,
        List<Long> placeIds,
        List<String> hashtags,
        PostVisibility visibility,
        Boolean publish
) {
}
