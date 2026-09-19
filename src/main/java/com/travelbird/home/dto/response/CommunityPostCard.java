package com.travelbird.home.dto.response;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.common.enums.CompanionType;
import com.travelbird.common.enums.TravelTheme;
import java.util.List;

public record CommunityPostCard(
        Long postId,
        String thumbnailUrl,
        String title,
        AuthorSummary author,
        RegionSummary region,
        CompanionType companionType,
        List<TravelTheme> themes,
        int viewCount,
        int saveCount,
        int shareCount,
        boolean savedRoute
) {
}
