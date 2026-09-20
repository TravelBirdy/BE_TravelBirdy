package com.travelbird.photomap.controller.dto;

import java.util.List;

/** backend-functional-spec-v10.md §3.11.2. */
public record PhotoMapPlaceListResponse(
        List<PhotoMapPlaceItem> places
) {
}
