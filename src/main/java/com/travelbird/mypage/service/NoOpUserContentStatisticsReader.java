package com.travelbird.mypage.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * {@link UserContentStatisticsReader}의 임시 구현.
 * Part 3의 실제 Post/PhotoMap 도메인이 이 저장소에 merge되면 같은 인터페이스의
 * 실제 구현 Bean이 생기므로, {@link ConditionalOnMissingBean}으로 그 시점에
 * merge 순서와 무관하게 자동으로 이 Bean이 밀려나도록 한다.
 */
@Component
@ConditionalOnMissingBean(UserContentStatisticsReader.class)
public class NoOpUserContentStatisticsReader implements UserContentStatisticsReader {

    @Override
    public UserContentStatistics getStatistics(Long userId) {
        return new UserContentStatistics(0, 0);
    }
}
