package com.travelbird.post.controller.dto;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.domain.PostVisibility;

import java.util.List;

/** backend-functional-spec-v10.md §3.8.3. */
public record PostDetailResponse(
        Long postId,
        AuthorSummary author,
        String title,
        String content,
        List<ImageSummary> images,
        List<PostDetailPlaceResponse> places,
        List<PostDetailRouteDay> route,
        PostVisibility visibility,
        long viewCount,
        long saveCount,
        long shareCount
) {
}
