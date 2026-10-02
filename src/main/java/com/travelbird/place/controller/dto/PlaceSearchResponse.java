package com.travelbird.place.controller.dto;

import com.travelbird.home.dto.response.PlaceSummary;

import java.util.List;

/** backend-functional-spec-v10.md §3.6.1. {@code isEnd}는 마지막 페이지 여부다. */
public record PlaceSearchResponse(List<PlaceSummary> items, int total, boolean isEnd) {
}
