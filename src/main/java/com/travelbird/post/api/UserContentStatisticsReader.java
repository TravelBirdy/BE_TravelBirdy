package com.travelbird.post.api;

/**
 * Part 3 → Part 1(마이페이지 집계) 공개 계약.
 * {@code postCount}는 DRAFT를 제외한다. (공통협의 4.3절 UserContentStatisticsReader)
 */
public interface UserContentStatisticsReader {

	UserContentStatistics getStatistics(Long userId);
}
