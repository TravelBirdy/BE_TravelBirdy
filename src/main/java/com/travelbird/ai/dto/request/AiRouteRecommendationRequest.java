package com.travelbird.ai.dto.request;
import jakarta.validation.constraints.NotNull;
public record AiRouteRecommendationRequest(@NotNull RequestType requestType,Boolean allowAdditionalRecommendations){public enum RequestType{TRIP_WISHLIST}public boolean allowAdditional(){return allowAdditionalRecommendations==null||allowAdditionalRecommendations;}}
