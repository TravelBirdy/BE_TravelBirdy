package com.travelbird.trip.dto.request;
import jakarta.validation.constraints.NotNull;
public record WishlistPlaceRequest(@NotNull Long placeId) {}
