package com.travelbird.post.api;

import java.util.List;

/**
 * Part 3 → Part 1(/api/home 추천 기록) 공개 계약.
 * BLOCKED·삭제·PRIVATE 게시글은 절대 반환하지 않는다. (공통협의 4.3절 HomePostReader)
 */
public interface HomePostReader {

	/**
	 * @param viewerIdOrNull {@code /api/home}은 비로그인도 허용되므로(security: [{}, {bearerAuth}])
	 *                       비로그인 요청은 {@code null}. {@code null}이면 {@link CommunityPostCard#savedRoute()}는
	 *                       항상 {@code false}로 반환한다.
	 */
	List<CommunityPostCard> getHomeRecommendedPosts(int limit, Long viewerIdOrNull);
}
