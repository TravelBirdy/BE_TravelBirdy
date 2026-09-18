package com.travelbird.ai.entity;
import static org.assertj.core.api.Assertions.*;import com.travelbird.global.error.AiJobFailureCode;import java.time.LocalDateTime;import org.junit.jupiter.api.Test;
class AiRecommendationJobTest{
 @Test void fiveStateLifecycleAndTimeoutAreDefinedFromPersistedStartedAt(){var now=LocalDateTime.of(2026,9,10,9,0);var job=AiRecommendationJob.queued(7L,now);job.start(now.plusSeconds(12));assertThat(job.getStatus()).isEqualTo(BackendAiJobStatus.PROCESSING);assertThat(job.callbackDeadline(180)).isEqualTo(now.plusSeconds(192));job.succeed(now.plusMinutes(1));assertThat(job.getStatus()).isEqualTo(BackendAiJobStatus.SUCCEEDED);job.expire(now.plusDays(1));assertThat(job.getStatus()).isEqualTo(BackendAiJobStatus.EXPIRED);}
 @Test void failureIsTerminal(){var now=LocalDateTime.of(2026,9,10,9,0);var job=AiRecommendationJob.queued(7L,now);job.fail(AiJobFailureCode.AI_PLACE_SYNC_FAILED,"sync failed",true,now.plusSeconds(1));assertThat(job.getStatus()).isEqualTo(BackendAiJobStatus.FAILED);assertThatThrownBy(()->job.start(now.plusSeconds(2))).isInstanceOf(IllegalStateException.class);}
}
