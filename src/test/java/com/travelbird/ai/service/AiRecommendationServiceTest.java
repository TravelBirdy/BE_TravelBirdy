package com.travelbird.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.dto.request.AiRecommendationRequest;
import com.travelbird.ai.repository.AiRecommendationJobRepository;
import com.travelbird.common.enums.*;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.repository.TripRepository;
import com.travelbird.user.api.UserReader;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class AiRecommendationServiceTest {

  @Test
  void queuesNormalizedSnapshotAndUsesKoreaMidnightForQuota() {
    var jobs = mock(AiRecommendationJobRepository.class);
    var users = mock(UserReader.class);
    var saved = mock(SavedPlaceReader.class);
    var trips = mock(TripRepository.class);
    var regions = mock(RegionReader.class);
    var places = mock(PlaceReader.class);
    var routeLocks = mock(PostRouteLockReader.class);
    var data = mock(AiRecommendationDataReader.class);
    var events = mock(ApplicationEventPublisher.class);
    when(regions.existsBySigunguCode("11110")).thenReturn(true);
    when(places.getPlaces(anyList(), eq(3L))).thenReturn(List.of());
    when(data.places(any())).thenReturn(List.of());
    var clock = Clock.fixed(Instant.parse("2026-09-10T00:00:00Z"), ZoneOffset.UTC);
    var mapper = new ObjectMapper().findAndRegisterModules();
    var service = new AiRecommendationService(
        jobs,
        users,
        saved,
        trips,
        regions,
        places,
        routeLocks,
        data,
        new RecommendationSnapshotCodec(mapper),
        new RecommendationPayloadFingerprint(mapper),
        events,
        clock);

    service.request(
        3L,
        new AiRecommendationRequest(
            "11110",
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            CompanionType.SOLO,
            List.of(TravelTheme.NATURE, TravelTheme.FOOD),
            Pace.NORMAL,
            AiRecommendationRequest.RequestType.GENERAL,
            null));

    verify(users).validateActiveUser(3L);
    verify(jobs).countSince(3L, LocalDateTime.of(2026, 9, 9, 15, 0));
    var capture = ArgumentCaptor.forClass(AiDispatchRequested.class);
    verify(events).publishEvent(capture.capture());
    assertThat(capture.getValue().request().themes())
        .containsExactly(TravelTheme.NATURE, TravelTheme.FOOD);
    assertThat(capture.getValue().request().savedPlaceIds()).isEmpty();
  }
}
