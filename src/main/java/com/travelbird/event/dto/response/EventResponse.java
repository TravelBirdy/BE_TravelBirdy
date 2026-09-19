package com.travelbird.event.dto.response;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.event.domain.EventStatus;
import java.time.LocalDate;

public record EventResponse(
        Long eventId,
        String name,
        RegionSummary region,
        String placeName,
        LocalDate startDate,
        LocalDate endDate,
        String thumbnailUrl,
        EventStatus status
) {
}
