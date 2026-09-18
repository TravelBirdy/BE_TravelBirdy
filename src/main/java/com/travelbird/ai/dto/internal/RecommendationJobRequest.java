package com.travelbird.ai.dto.internal;

import com.travelbird.ai.entity.AiRequestType;
import com.travelbird.trip.entity.CompanionType;
import com.travelbird.trip.entity.Pace;
import com.travelbird.trip.entity.TravelTheme;
import java.time.LocalDate;
import java.util.List;

public record RecommendationJobRequest(Long jobId, AiRequestType requestType, String regionCode,
    LocalDate startDate, LocalDate endDate, CompanionType companionType, List<TravelTheme> themes,
    Pace pace, List<Long> savedPlaceIds, List<Long> wishlistPlaceIds,
    List<ExistingScheduleDay> existingSchedule, boolean allowAdditionalRecommendations) {
  public RecommendationJobRequest {
    themes = List.copyOf(themes);
    savedPlaceIds = List.copyOf(savedPlaceIds);
    wishlistPlaceIds = List.copyOf(wishlistPlaceIds);
    existingSchedule = List.copyOf(existingSchedule);
  }
}
