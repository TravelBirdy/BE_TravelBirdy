package com.travelbird.event.dto.response;

import java.util.List;

public record EventListResponse(
        List<EventResponse> items,
        Long nextCursor
) {
}
