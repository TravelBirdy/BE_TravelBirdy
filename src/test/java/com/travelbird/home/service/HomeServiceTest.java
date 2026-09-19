package com.travelbird.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.client.WeatherClient;
import com.travelbird.home.domain.WeatherType;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private WeatherClient weatherClient;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(weatherClient);
    }

    @Test
    void getHome_noLocation_usesSeoulDefault() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null);

        assertThat(response.weather().baseLocation()).isEqualTo("서울특별시");
        assertThat(response.unavailableSections()).doesNotContain("WEATHER");
    }

    @Test
    void getHome_withLocation_usesClientCoordinates() {
        when(weatherClient.getWeather(eq(new BigDecimal("35.0")), eq(new BigDecimal("129.0")), eq("현재 위치")))
                .thenReturn(new WeatherSummary(WeatherType.RAINY, "비", "현재 위치"));

        var response = homeService.getHome(new BigDecimal("35.0"), new BigDecimal("129.0"));

        assertThat(response.weather().baseLocation()).isEqualTo("현재 위치");
    }

    @Test
    void getHome_weatherClientFails_marksWeatherUnavailable() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(null);

        var response = homeService.getHome(null, null);

        assertThat(response.weather()).isNull();
        assertThat(response.unavailableSections()).contains("WEATHER");
    }

    @Test
    void getHome_placesPostsAndEventsAlwaysUnavailable() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null);

        assertThat(response.recommendedPlaces()).isEmpty();
        assertThat(response.recommendedPosts()).isEmpty();
        assertThat(response.monthlyEvents()).isEmpty();
        assertThat(response.unavailableSections()).contains("PLACES", "POSTS", "EVENTS");
    }
}
