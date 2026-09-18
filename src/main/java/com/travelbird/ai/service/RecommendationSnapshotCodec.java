package com.travelbird.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.dto.internal.*;
import com.travelbird.ai.entity.AiRecommendationJob;
import com.travelbird.trip.entity.TravelTheme;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecommendationSnapshotCodec {
  private final ObjectMapper json;
  public RecommendationSnapshotCodec(ObjectMapper json) { this.json = json; }
  public String write(Object value) { try { return json.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }
  public RecommendationJobRequest read(AiRecommendationJob job) {
    try {
      return new RecommendationJobRequest(job.getId(), job.getRequestType(), job.getRegionCode(),
          job.getStartDate(), job.getEndDate(), job.getCompanionType(),
          json.readValue(job.getThemes(), new TypeReference<List<TravelTheme>>() {}), job.getPace(),
          json.readValue(job.getSavedPlaceIds(), new TypeReference<List<Long>>() {}),
          json.readValue(job.getWishlistPlaceIds(), new TypeReference<List<Long>>() {}),
          json.readValue(job.getExistingSchedule(), new TypeReference<List<ExistingScheduleDay>>() {}),
          job.isAllowAdditionalRecommendations());
    } catch (JsonProcessingException e) { throw new IllegalStateException("Invalid stored AI snapshot", e); }
  }
}
