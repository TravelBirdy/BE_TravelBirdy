package com.travelbird.post.controller.dto;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;

import java.time.LocalDateTime;

/** backend-functional-spec-v10.md §3.8.3. */
public record MyPostItem(
        Long postId,
        Long tripId,
        String title,
        String thumbnailUrl,
        RegionSummary region,
        PostStatus status,
        PostVisibility visibility,
        LocalDateTime createdAt
) {
}
