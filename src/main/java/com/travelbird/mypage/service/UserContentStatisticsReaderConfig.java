package com.travelbird.mypage.service;

import com.travelbird.post.api.UserContentStatisticsReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Part 3가 {@code post.api.UserContentStatisticsReader}의 실제 구현 Bean을 등록하면,
 * {@code @ConditionalOnMissingBean}으로 merge 순서와 무관하게
 * {@link NoOpUserContentStatisticsReader}가 자동으로 밀려난다.
 */
@Configuration
public class UserContentStatisticsReaderConfig {

    @Bean
    @ConditionalOnMissingBean(UserContentStatisticsReader.class)
    public UserContentStatisticsReader userContentStatisticsReader() {
        return new NoOpUserContentStatisticsReader();
    }
}
