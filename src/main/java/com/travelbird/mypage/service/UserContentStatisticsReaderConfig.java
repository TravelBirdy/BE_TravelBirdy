package com.travelbird.mypage.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Part 3의 실제 Post/PhotoMap 도메인이 이 저장소에 merge되면 같은 인터페이스의
 * 실제 구현 Bean이 생기므로, {@code @ConditionalOnMissingBean}으로 그 시점에
 * merge 순서와 무관하게 {@link NoOpUserContentStatisticsReader}가 자동으로 밀려나도록 한다.
 */
@Configuration
public class UserContentStatisticsReaderConfig {

    @Bean
    @ConditionalOnMissingBean(UserContentStatisticsReader.class)
    public UserContentStatisticsReader userContentStatisticsReader() {
        return new NoOpUserContentStatisticsReader();
    }
}
