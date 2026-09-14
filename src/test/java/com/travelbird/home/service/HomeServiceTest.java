package com.travelbird.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.common.service.RegionReader;
import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.client.WeatherClient;
import com.travelbird.event.domain.Event;
import com.travelbird.event.domain.EventStatus;
import com.travelbird.home.domain.WeatherType;
import com.travelbird.event.repository.EventRepository;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private RegionReader regionReader;
    @Mock
    private WeatherClient weatherClient;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(eventRepository, regionReader, weatherClient);
        when(eventRepository.findOngoingOrUpcomingWithinMonth(any(), any())).thenReturn(List.of());
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
        when(weatherClient.getWeather(any(), any(), any())).thenReturn(null);

        var response = homeService.getHome(null, null);

        assertThat(response.weather()).isNull();
        assertThat(response.unavailableSections()).contains("WEATHER");
    }

    @Test
    void getHome_placesAndPostsAlwaysUnavailable() {
        when(weatherClient.getWeather(any(), any(), any())).thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null);

        assertThat(response.recommendedPlaces()).isEmpty();
        assertThat(response.recommendedPosts()).isEmpty();
        assertThat(response.unavailableSections()).contains("PLACES", "POSTS");
    }

    @Test
    void getHome_mapsEventStatusAndRegion() {
        LocalDate today = LocalDate.now();
        Event ongoing = newEvent(1L, "축제A", "11110", today.minusDays(1), today.plusDays(1));
        Event upcoming = newEvent(2L, "축제B", "26110", today.plusDays(3), today.plusDays(5));
        when(eventRepository.findOngoingOrUpcomingWithinMonth(any(), any())).thenReturn(List.of(ongoing, upcoming));
        when(regionReader.getRegions(any())).thenReturn(List.of(
                new RegionSummary("11110", "종로구"), new RegionSummary("26110", "중구")));
        when(weatherClient.getWeather(any(), any(), any())).thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null);

        assertThat(response.monthlyEvents()).hasSize(2);
        assertThat(response.monthlyEvents().get(0).status()).isEqualTo(EventStatus.ONGOING);
        assertThat(response.monthlyEvents().get(0).region().sigunguName()).isEqualTo("종로구");
        assertThat(response.monthlyEvents().get(1).status()).isEqualTo(EventStatus.UPCOMING);
        assertThat(response.monthlyEvents().get(1).region().sigunguName()).isEqualTo("중구");
    }

    private Event newEvent(Long id, String name, String sigunguCode, LocalDate start, LocalDate end) {
        try {
            Constructor<Event> constructor = Event.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Event event = constructor.newInstance();
            ReflectionTestUtils.setField(event, "eventId", id);
            ReflectionTestUtils.setField(event, "name", name);
            ReflectionTestUtils.setField(event, "sigunguCode", sigunguCode);
            ReflectionTestUtils.setField(event, "placeName", "장소");
            ReflectionTestUtils.setField(event, "startDate", start);
            ReflectionTestUtils.setField(event, "endDate", end);
            return event;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
