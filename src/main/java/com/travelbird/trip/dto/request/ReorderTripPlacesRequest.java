package com.travelbird.trip.dto.request;
import jakarta.validation.constraints.NotNull;
import java.util.List;
public record ReorderTripPlacesRequest(@NotNull List<Long> orderedTripPlaceIds) {}
