package com.travelbird.post.api;

import java.util.List;

/**
 * Part 3 → Part 1(/api/home 추천 기록) 공개 계약.
 * BLOCKED·삭제·PRIVATE 게시글은 절대 반환하지 않는다. (공통협의 4.3절 HomePostReader)
 */
public interface HomePostReader {

	List<HomePostCard> getHomeRecommendedPosts(int limit);
}
