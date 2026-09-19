package com.travelbird.social.dto.response;

import java.util.List;

public record FollowListResponse(
        List<FollowUserItem> items,
        Long nextCursor
) {
}
