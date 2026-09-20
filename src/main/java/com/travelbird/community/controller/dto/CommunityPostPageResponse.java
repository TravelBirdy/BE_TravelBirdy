package com.travelbird.community.controller.dto;

import com.travelbird.post.api.CommunityPostCard;

import java.util.List;

/** 커뮤니티 목록·검색 공용 응답. backend-functional-spec-v10.md §3.9.1/§3.9.3. */
public record CommunityPostPageResponse(
        List<CommunityPostCard> items,
        Long nextCursor
) {
}
