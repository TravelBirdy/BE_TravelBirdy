package com.travelbird.post.api;

import com.travelbird.common.dto.RegionSummary;

import java.util.List;

/**
 * 커뮤니티 게시글 카드. {@code travelbird-openapi-v10.json}의
 * {@code HomeResponse.recommendedPosts}가 이 타입을 그대로 참조하므로(Home과 커뮤니티가
 * 같은 카드 모양을 공유), Home 추천({@link HomePostReader})과 커뮤니티 목록/검색이 이
 * 하나의 타입을 함께 쓴다.
 *
 * <p>{@code companionType}/{@code themes}는 {@code trips}(Part2) 컬럼이라 커뮤니티 목록(3.9)에서
 * 쓰는 조인을 재사용해서 채운다. 공통 Enum({@code CompanionType}, {@code TravelTheme}) 소유
 * 패키지가 아직 없어 임시로 {@code String}/{@code List<String>}으로 둔다.
 */
public record CommunityPostCard(
		Long postId,
		String thumbnailUrl,
		String title,
		AuthorSummary author,
		RegionSummary region,
		String companionType,
		List<String> themes,
		long viewCount,
		long saveCount,
		long shareCount,
		boolean savedRoute
) {
}
