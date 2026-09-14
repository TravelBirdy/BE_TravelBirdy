package com.travelbird.mypage.service;

/**
 * Part 3 → Part 1(마이페이지 집계) 공개 계약 (공통협의 4.3절).
 * postCount는 DRAFT/삭제/BLOCKED(본인 게시글 포함) 제외, visitedRegionCount는
 * 5자리 시군구 기준. Part 3 Post/PhotoMap 도메인이 이 저장소에 merge되기
 * 전까지는 {@link NoOpUserContentStatisticsReader}가 자리를 대신한다.
 */
public interface UserContentStatisticsReader {

    UserContentStatistics getStatistics(Long userId);
}
