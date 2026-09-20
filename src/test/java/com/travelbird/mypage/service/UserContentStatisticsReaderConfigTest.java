package com.travelbird.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.travelbird.post.api.UserContentStatistics;
import com.travelbird.post.api.UserContentStatisticsReader;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.stereotype.Component;

class UserContentStatisticsReaderConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    void Part3_실구현이_없으면_NoOp이_등록된다() {
        contextRunner
                .withUserConfiguration(UserContentStatisticsReaderConfig.class)
                .run(context -> {
                    assertThat(context.getBeansOfType(UserContentStatisticsReader.class)).hasSize(1);
                    UserContentStatisticsReader reader = context.getBean(UserContentStatisticsReader.class);
                    assertThat(reader).isInstanceOf(NoOpUserContentStatisticsReader.class);
                    assertThat(reader.getStatistics(1L)).isEqualTo(new UserContentStatistics(0, 0));
                });
    }

    @Test
    void Part3_실구현이_등록되면_NoOp은_밀려난다() {
        contextRunner
                .withUserConfiguration(FakePart3Reader.class, UserContentStatisticsReaderConfig.class)
                .run(context -> {
                    assertThat(context.getBeansOfType(UserContentStatisticsReader.class)).hasSize(1);
                    UserContentStatisticsReader reader = context.getBean(UserContentStatisticsReader.class);
                    assertThat(reader).isInstanceOf(FakePart3Reader.class);
                    assertThat(reader.getStatistics(1L)).isEqualTo(new UserContentStatistics(7, 2));
                });
    }

    @Component
    static class FakePart3Reader implements UserContentStatisticsReader {

        @Override
        public UserContentStatistics getStatistics(Long userId) {
            return new UserContentStatistics(7, 2);
        }
    }
}
