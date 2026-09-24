package com.travelbird.post.controller.dto;

import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;

import java.time.LocalDateTime;

/** backend-functional-spec-v10.md §3.8.1. */
public record CreatePostResponse(
        Long postId,
        PostStatus status,
        PostVisibility visibility,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        boolean tripRouteLocked
) {
}
