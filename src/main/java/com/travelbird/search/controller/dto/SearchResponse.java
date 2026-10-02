package com.travelbird.search.controller.dto;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.home.dto.response.PlaceSummary;

import java.util.List;

/** backend-functional-spec-v10.md §3.16.1. 표시 순서는 places, posts, regions. */
public record SearchResponse(
        List<PlaceSummary> places,
        List<CommunityPostCard> posts,
        List<RegionSummary> regions,
        boolean hasMorePlaces,
        boolean hasMorePosts,
        boolean hasMoreRegions,
        List<String> unavailableSections
) {
}
