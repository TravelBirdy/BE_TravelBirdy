package com.travelbird.home.dto.response;

import com.travelbird.event.dto.response.EventResponse;
import java.util.List;

public record HomeResponse(
        WeatherSummary weather,
        List<PlaceSummary> recommendedPlaces,
        List<EventResponse> monthlyEvents,
        List<CommunityPostCard> recommendedPosts,
        List<String> unavailableSections
) {
}
