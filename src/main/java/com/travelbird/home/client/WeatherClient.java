package com.travelbird.home.client;

import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.domain.WeatherType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Open-Meteo Forecast API 연동. 장애 시 예외를 던지지 않고 null을 반환한다
 * (기능명세서 3.12.1: Open-Meteo 장애가 홈 전체를 실패시키면 안 됨).
 */
@Component
public class WeatherClient {

    private static final int TIMEOUT_MILLIS = 3000;

    // https://open-meteo.com/en/docs 의 WMO Weather interpretation codes 표준 매핑.
    private static final Set<Integer> CLOUDY_CODES = Set.of(1, 2, 3);
    private static final Set<Integer> FOGGY_CODES = Set.of(45, 48);
    private static final Set<Integer> RAINY_CODES = Set.of(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82);
    private static final Set<Integer> SNOWY_CODES = Set.of(71, 73, 75, 77, 85, 86);
    private static final Set<Integer> THUNDERSTORM_CODES = Set.of(95, 96, 99);

    private static final Map<WeatherType, String> WEATHER_TEXT = Map.of(
            WeatherType.CLEAR, "맑음",
            WeatherType.CLOUDY, "흐림",
            WeatherType.FOGGY, "안개",
            WeatherType.RAINY, "비",
            WeatherType.SNOWY, "눈",
            WeatherType.THUNDERSTORM, "천둥번개"
    );

    private final RestClient restClient;
    private final String forecastBaseUrl;

    public WeatherClient(@Value("${open-meteo.forecast-uri}") String forecastBaseUrl) {
        this.forecastBaseUrl = forecastBaseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public WeatherSummary getWeather(BigDecimal latitude, BigDecimal longitude, String baseLocation) {
        try {
            OpenMeteoResponse response = restClient.get()
                    .uri(forecastBaseUrl + "?latitude={lat}&longitude={lon}&current=weather_code", latitude, longitude)
                    .retrieve()
                    .body(OpenMeteoResponse.class);

            if (response == null || response.current() == null || response.current().weatherCode() == null) {
                return null;
            }
            WeatherType weatherType = toWeatherType(response.current().weatherCode());
            return new WeatherSummary(weatherType, WEATHER_TEXT.get(weatherType), baseLocation);
        } catch (RestClientException e) {
            return null;
        }
    }

    private WeatherType toWeatherType(int wmoCode) {
        if (wmoCode == 0) {
            return WeatherType.CLEAR;
        }
        if (CLOUDY_CODES.contains(wmoCode)) {
            return WeatherType.CLOUDY;
        }
        if (FOGGY_CODES.contains(wmoCode)) {
            return WeatherType.FOGGY;
        }
        if (RAINY_CODES.contains(wmoCode)) {
            return WeatherType.RAINY;
        }
        if (SNOWY_CODES.contains(wmoCode)) {
            return WeatherType.SNOWY;
        }
        if (THUNDERSTORM_CODES.contains(wmoCode)) {
            return WeatherType.THUNDERSTORM;
        }
        return WeatherType.CLEAR;
    }
}
