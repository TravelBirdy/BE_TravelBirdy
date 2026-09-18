package com.travelbird.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.dto.internal.ExistingScheduleDay;
import com.travelbird.ai.dto.internal.RecommendationJobRequest;
import com.travelbird.ai.entity.AiRequestType;
import com.travelbird.trip.entity.CompanionType;
import com.travelbird.trip.entity.Pace;
import com.travelbird.trip.entity.TravelTheme;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationPayloadFingerprintTest {
  private final RecommendationPayloadFingerprint fingerprint =
      new RecommendationPayloadFingerprint(new ObjectMapper().findAndRegisterModules());

  @Test
  void ignoresJobIdAndOrderInsensitiveCollectionsButPreservesPlaceOrderWithinDay() {
    var first = request(1L, List.of(TravelTheme.FOOD, TravelTheme.NATURE), List.of(3L, 2L),
        List.of(new ExistingScheduleDay(2, List.of(8L)), new ExistingScheduleDay(1, List.of(5L, 6L))));
    var equivalent = request(99L, List.of(TravelTheme.NATURE, TravelTheme.FOOD), List.of(2L, 3L),
        List.of(new ExistingScheduleDay(1, List.of(5L, 6L)), new ExistingScheduleDay(2, List.of(8L))));
    var differentOrder = request(1L, first.themes(), first.savedPlaceIds(),
        List.of(new ExistingScheduleDay(1, List.of(6L, 5L)), new ExistingScheduleDay(2, List.of(8L))));

    assertThat(fingerprint.sha256(first)).isEqualTo(fingerprint.sha256(equivalent));
    assertThat(fingerprint.sha256(first)).isNotEqualTo(fingerprint.sha256(differentOrder));
  }

  private RecommendationJobRequest request(Long jobId, List<TravelTheme> themes,
      List<Long> saved, List<ExistingScheduleDay> schedule) {
    return new RecommendationJobRequest(jobId, AiRequestType.SAVED_PLACES, "11110",
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), CompanionType.FRIENDS,
        themes, Pace.NORMAL, saved, List.of(11L, 10L), schedule, true);
  }
}
