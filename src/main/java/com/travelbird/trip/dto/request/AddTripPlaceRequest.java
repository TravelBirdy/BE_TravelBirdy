package com.travelbird.trip.dto.request;
import jakarta.validation.constraints.NotNull;
public record AddTripPlaceRequest(@NotNull Long placeId, Integer order) {}
