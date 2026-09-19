package com.travelbird.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.entity.*;
import com.travelbird.ai.repository.*;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.repository.TripRepository;
import com.travelbird.user.api.UserReader;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class AiRecommendationStatusPreviewTest {

  @Test
  void succeededJobExposesPreviewLifecycle() {
    var jobs = mock(AiRecommendationJobRepository.class);
    var job = mock(AiRecommendationJob.class);
    when(jobs.findById(7L)).thenReturn(Optional.of(job));
    when(job.ownedBy(3L)).thenReturn(true);
    when(job.getStatus()).thenReturn(BackendAiJobStatus.SUCCEEDED);
    var previews = mock(AiTripPreviewRepository.class);
    var preview = mock(AiTripPreview.class);
    when(previews.findByJobId(7L)).thenReturn(Optional.of(preview));
    when(preview.getId()).thenReturn(9L);
    when(preview.getRetentionStatus()).thenReturn(AiPreviewRetentionStatus.PERMANENT);
    var mapper = new ObjectMapper().findAndRegisterModules();
    var service = new AiRecommendationService(
        jobs,
        mock(UserReader.class),
        mock(SavedPlaceReader.class),
        mock(TripRepository.class),
        mock(RegionReader.class),
        mock(PlaceReader.class),
        mock(PostRouteLockReader.class),
        mock(AiRecommendationDataReader.class),
        new RecommendationSnapshotCodec(mapper),
        new RecommendationPayloadFingerprint(mapper),
        mock(ApplicationEventPublisher.class),
        Clock.systemUTC());
    service.setPreviewRepository(previews);

    var response = service.status(3L, 7L);

    assertThat(response.previewId()).isEqualTo(9L);
    assertThat(response.previewAvailable()).isTrue();
    assertThat(response.previewRetentionStatus()).isEqualTo("PERMANENT");
    assertThat(response.editable()).isTrue();
  }
}
