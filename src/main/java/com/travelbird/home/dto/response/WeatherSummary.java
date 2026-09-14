package com.travelbird.home.dto.response;

import com.travelbird.home.domain.WeatherType;

public record WeatherSummary(
        WeatherType weatherType,
        String text,
        String baseLocation
) {
}
