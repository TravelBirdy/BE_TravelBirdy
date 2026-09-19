package com.travelbird.ai.service;
import com.travelbird.ai.dto.internal.*;
public record AiDispatchRequested(RecommendationJobRequest request, PlaceSyncRequest syncRequest, String fingerprint) {}
