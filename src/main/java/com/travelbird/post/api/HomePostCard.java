package com.travelbird.post.api;

import com.travelbird.common.dto.RegionSummary;

import java.util.List;

/**
 * {@code travelbird-openapi-v10.json}의 {@code HomeResponse.recommendedPosts}가
 * {@code CommunityPostCard}를 그대로 참조하므로 그 required 필드셋에 맞춘다.
 *
 * <p>{@code companionType}/{@code themes}는 {@code trips}(Part2) 컬럼이라 커뮤니티 목록(3.9)에서
 * 쓰는 조인을 재사용해서 채운다. 공통 Enum({@code CompanionType}, {@code TravelTheme}) 소유
 * 패키지가 아직 없어 임시로 {@code String}/{@code List<String>}으로 둔다.
 */
public record HomePostCard(
		Long postId,
		String title,
		String thumbnailUrl,
		RegionSummary region,
		AuthorSummary author,
		String companionType,
		List<String> themes,
		long viewCount,
		long saveCount,
		long shareCount,
		boolean savedRoute
) {
}
