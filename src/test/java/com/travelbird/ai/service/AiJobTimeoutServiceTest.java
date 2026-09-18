package com.travelbird.ai.service;

import static org.mockito.Mockito.*;

import com.travelbird.ai.entity.BackendAiJobStatus;
import com.travelbird.ai.repository.AiRecommendationJobRepository;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiJobTimeoutServiceTest {
  @Test
  void marksOnlyProcessingJobsPastDeadlineAsCallbackTimeout() {
    var jobs = mock(AiRecommendationJobRepository.class);
    var job = mock(com.travelbird.ai.entity.AiRecommendationJob.class);
    var now = LocalDateTime.of(2026, 9, 10, 12, 0);
    when(jobs.findByStatusAndStartedAtLessThanEqual(BackendAiJobStatus.PROCESSING,
        now.minusSeconds(180))).thenReturn(List.of(job));
    when(jobs.findByIdForUpdate(job.getId())).thenReturn(java.util.Optional.of(job));
    when(job.getStatus()).thenReturn(BackendAiJobStatus.PROCESSING);
    when(job.getStartedAt()).thenReturn(now.minusSeconds(180));

    new AiJobTimeoutService(jobs, Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), 180)
        .failTimedOutJobs();

    verify(job).fail(com.travelbird.global.error.AiJobFailureCode.AI_CALLBACK_TIMEOUT,
        "AI callback was not received within 180 seconds", true, now);
  }
}
