package com.travelbird.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.home.client.WeatherClient;
import com.travelbird.home.domain.HomeRecommendedPlace;
import com.travelbird.home.domain.WeatherType;
import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.repository.HomeRecommendedPlaceRepository;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.domain.PlaceStatus;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.HomePostReader;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
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
    private WeatherClient weatherClient;
    @Mock
    private HomeRecommendedPlaceRepository homeRecommendedPlaceRepository;
    @Mock
    private PlaceReader placeReader;
    @Mock
    private HomePostReader homePostReader;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService(weatherClient, homeRecommendedPlaceRepository, placeReader, homePostReader);
    }

    private HomeRecommendedPlace recommendedPlace(Long placeId, int displayOrder) {
        HomeRecommendedPlace place = newInstance(HomeRecommendedPlace.class);
        ReflectionTestUtils.setField(place, "placeId", placeId);
        ReflectionTestUtils.setField(place, "displayOrder", displayOrder);
        return place;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getHome_noLocation_usesSeoulDefault() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null, null);

        assertThat(response.weather().baseLocation()).isEqualTo("서울특별시");
        assertThat(response.unavailableSections()).doesNotContain("WEATHER");
    }

    @Test
    void getHome_withLocation_usesClientCoordinates() {
        when(weatherClient.getWeather(eq(new BigDecimal("35.0")), eq(new BigDecimal("129.0")), eq("현재 위치")))
                .thenReturn(new WeatherSummary(WeatherType.RAINY, "비", "현재 위치"));

        var response = homeService.getHome(new BigDecimal("35.0"), new BigDecimal("129.0"), null);

        assertThat(response.weather().baseLocation()).isEqualTo("현재 위치");
    }

    @Test
    void getHome_weatherClientFails_marksWeatherUnavailable() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(null);

        var response = homeService.getHome(null, null, null);

        assertThat(response.weather()).isNull();
        assertThat(response.unavailableSections()).contains("WEATHER");
    }

    @Test
    void getHome_eventsAlwaysUnavailable() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));

        var response = homeService.getHome(null, null, null);

        assertThat(response.monthlyEvents()).isEmpty();
        assertThat(response.unavailableSections()).contains("EVENTS");
    }

    @Test
    void getHome_noAdminRecommendedPlaces_returnsEmptyWithoutMarkingUnavailable() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));
        when(homeRecommendedPlaceRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());

        var response = homeService.getHome(null, null, null);

        assertThat(response.recommendedPlaces()).isEmpty();
        assertThat(response.unavailableSections()).doesNotContain("PLACES");
        verify(placeReader, never()).getPlaces(anyList(), eq((Long) null));
    }

    @Test
    void getHome_adminRecommendedPlacesExist_returnsThemInDisplayOrder() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));
        when(homeRecommendedPlaceRepository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(recommendedPlace(20L, 2), recommendedPlace(10L, 1)));
        PlaceContract first = new PlaceContract(10L, "경복궁", "서울 종로구", "11110",
                PlaceCategory.ATTRACTION, new BigDecimal("37.5"), new BigDecimal("126.9"), PlaceStatus.ACTIVE, false);
        PlaceContract second = new PlaceContract(20L, "해운대", "부산 해운대구", "26440",
                PlaceCategory.NATURE, new BigDecimal("35.1"), new BigDecimal("129.1"), PlaceStatus.ACTIVE, false);
        when(placeReader.getPlaces(List.of(20L, 10L), null)).thenReturn(List.of(first, second));

        var response = homeService.getHome(null, null, null);

        assertThat(response.recommendedPlaces()).extracting("placeId").containsExactly(20L, 10L);
        assertThat(response.unavailableSections()).doesNotContain("PLACES");
    }

    @Test
    void getHome_homePostReaderReturnsPosts_passesThroughAsRecommendedPosts() {
        when(weatherClient.getWeather(eq(new BigDecimal("37.5665")), eq(new BigDecimal("126.9780")), eq("서울특별시")))
                .thenReturn(new WeatherSummary(WeatherType.CLEAR, "맑음", "서울특별시"));
        when(homeRecommendedPlaceRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        CommunityPostCard card = new CommunityPostCard(1L, "thumb", "제목", null, null, null, List.of(), 1, 1, 1, false);
        when(homePostReader.getHomeRecommendedPosts(7, 5L)).thenReturn(List.of(card));

        var response = homeService.getHome(null, null, 5L);

        assertThat(response.recommendedPosts()).containsExactly(card);
        assertThat(response.unavailableSections()).doesNotContain("POSTS");
    }
}
