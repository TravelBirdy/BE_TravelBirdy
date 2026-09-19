package com.travelbird.mypage.dto.response;

public record MyPageStatistics(
        long postCount,
        long visitedRegionCount,
        long followerCount,
        long followingCount
) {
}
