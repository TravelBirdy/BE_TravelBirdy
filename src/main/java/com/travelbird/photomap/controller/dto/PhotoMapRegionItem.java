package com.travelbird.photomap.controller.dto;

import com.travelbird.common.dto.RegionSummary;

/** backend-functional-spec-v10.md §3.11.1. */
public record PhotoMapRegionItem(
        RegionSummary region,
        long visitedPlaceCount,
        long recordCount
) {
}
