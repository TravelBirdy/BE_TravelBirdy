package com.travelbird.mypage.service;

import org.springframework.stereotype.Component;

/**
 * {@link UserContentStatisticsReader}의 임시 구현.
 * Part 3의 실제 Post/PhotoMap 도메인이 이 저장소에 merge되면 이 Bean을
 * 지우고 Part 3가 제공하는 실제 구현으로 교체한다 (공통협의 섹션4의
 * Mock/Fake 병행 개발 규칙).
 */
@Component
public class NoOpUserContentStatisticsReader implements UserContentStatisticsReader {

    @Override
    public UserContentStatistics getStatistics(Long userId) {
        return new UserContentStatistics(0, 0);
    }
}
