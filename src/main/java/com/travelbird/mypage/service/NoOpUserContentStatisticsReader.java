package com.travelbird.mypage.service;

import com.travelbird.post.api.UserContentStatistics;
import com.travelbird.post.api.UserContentStatisticsReader;

/**
 * Part 3 공식 계약 {@link UserContentStatisticsReader}(공통협의 4.3절)의 임시 구현. Bean 등록은
 * {@link UserContentStatisticsReaderConfig}에서 {@code @ConditionalOnMissingBean}으로
 * 처리한다(이 클래스 자체를 {@code @Component}로 스캔하면 조건 평가가 신뢰할 수 없어서
 * 별도 Config 클래스로 분리함).
 */
public class NoOpUserContentStatisticsReader implements UserContentStatisticsReader {

    @Override
    public UserContentStatistics getStatistics(Long userId) {
        return new UserContentStatistics(0, 0);
    }
}
