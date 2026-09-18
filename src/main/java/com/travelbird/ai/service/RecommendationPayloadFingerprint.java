package com.travelbird.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.dto.internal.ExistingScheduleDay;
import com.travelbird.ai.dto.internal.RecommendationJobRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RecommendationPayloadFingerprint {
  private final ObjectMapper objectMapper;

  public RecommendationPayloadFingerprint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String sha256(RecommendationJobRequest request) {
    var normalized = new RecommendationJobRequest(null, request.requestType(), request.regionCode(),
        request.startDate(), request.endDate(), request.companionType(),
        request.themes().stream().sorted().toList(), request.pace(),
        request.savedPlaceIds().stream().sorted().toList(),
        request.wishlistPlaceIds().stream().sorted().toList(),
        request.existingSchedule().stream().sorted(Comparator.comparingInt(ExistingScheduleDay::dayNumber)).toList(),
        request.allowAdditionalRecommendations());
    try {
      byte[] json = objectMapper.writeValueAsBytes(normalized);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Cannot fingerprint recommendation payload", e);
    }
  }
}
