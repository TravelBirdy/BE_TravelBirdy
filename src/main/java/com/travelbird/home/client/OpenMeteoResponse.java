package com.travelbird.home.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record OpenMeteoResponse(
        Current current
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Current(
            @JsonProperty("weather_code") Integer weatherCode
    ) {
    }
}
