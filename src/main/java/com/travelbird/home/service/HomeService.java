package com.travelbird.home.service;

import com.travelbird.home.dto.response.HomeResponse;
import com.travelbird.home.dto.response.PlaceSummary;
import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.client.WeatherClient;
import com.travelbird.home.domain.HomeRecommendedPlace;
import com.travelbird.home.repository.HomeRecommendedPlaceRepository;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.HomePostReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final BigDecimal SEOUL_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal SEOUL_LONGITUDE = new BigDecimal("126.9780");

    /** 기능명세서 §3.12.1 "오늘의 추천 장소 7개" — 운영자가 더 많이 지정해도 displayOrder 앞에서 7개만 반환한다. */
    private static final int HOME_RECOMMENDED_PLACE_LIMIT = 7;

    /** 추천 기록 개수는 명세에 별도 지정이 없어 추천 장소와 동일하게 맞춘다. */
    private static final int HOME_RECOMMENDED_POST_LIMIT = 7;

    private final WeatherClient weatherClient;
    private final HomeRecommendedPlaceRepository homeRecommendedPlaceRepository;
    private final PlaceReader placeReader;
    private final HomePostReader homePostReader;

    public HomeService(
            WeatherClient weatherClient,
            HomeRecommendedPlaceRepository homeRecommendedPlaceRepository,
            PlaceReader placeReader,
            HomePostReader homePostReader
    ) {
        this.weatherClient = weatherClient;
        this.homeRecommendedPlaceRepository = homeRecommendedPlaceRepository;
        this.placeReader = placeReader;
        this.homePostReader = homePostReader;
    }

    public HomeResponse getHome(BigDecimal latitude, BigDecimal longitude, Long viewerIdOrNull) {
        List<String> unavailableSections = new ArrayList<>();

        boolean hasClientLocation = latitude != null && longitude != null;
        BigDecimal resolvedLatitude = hasClientLocation ? latitude : SEOUL_LATITUDE;
        BigDecimal resolvedLongitude = hasClientLocation ? longitude : SEOUL_LONGITUDE;
        String baseLocation = hasClientLocation ? "현재 위치" : "서울특별시";

        WeatherSummary weather = weatherClient.getWeather(resolvedLatitude, resolvedLongitude, baseLocation);
        if (weather == null) {
            unavailableSections.add("WEATHER");
        }

        List<PlaceSummary> recommendedPlaces = getRecommendedPlaces(viewerIdOrNull);
        List<CommunityPostCard> recommendedPosts =
                homePostReader.getHomeRecommendedPosts(HOME_RECOMMENDED_POST_LIMIT, viewerIdOrNull);

        // 축제 동기화 정책(TourAPI 검증)이 확정되어 별도 PR로 merge되기 전까지 항상 비움.
        unavailableSections.add("EVENTS");

        return new HomeResponse(weather, recommendedPlaces, List.of(), recommendedPosts, unavailableSections);
    }

    /**
     * 운영자가 {@code home_recommended_places}에 직접 지정한 장소를 {@code displayOrder} 순서로
     * 조회해 최대 {@link #HOME_RECOMMENDED_PLACE_LIMIT}개만 반환한다(§3.12.1 "MVP 추천 장소 7개는
     * 운영자가 DB에서 직접 지정한 고정 장소다"). 개수 제한은 {@link PlaceReader}가 돌려주지 않은
     * 장소를 걸러낸 뒤에 적용해서, 지정된 장소 일부가 조회되지 않아도 뒤 순서 장소로 채워진다.
     * 빈 결과는 아직 운영자가 지정을 안 한 정상적인 상태이지, 실패가 아니라서
     * {@code unavailableSections}에 올리지 않는다 — 날씨가 실제 장애일 때만 올리는 것과 같은 원칙.
     */
    private List<PlaceSummary> getRecommendedPlaces(Long viewerIdOrNull) {
        List<Long> orderedPlaceIds = homeRecommendedPlaceRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(HomeRecommendedPlace::getPlaceId)
                .toList();
        if (orderedPlaceIds.isEmpty()) {
            return List.of();
        }
        Map<Long, PlaceContract> placesById = placeReader.getPlaces(orderedPlaceIds, viewerIdOrNull).stream()
                .collect(Collectors.toMap(PlaceContract::placeId, Function.identity()));
        return orderedPlaceIds.stream()
                .map(placesById::get)
                .filter(place -> place != null)
                .limit(HOME_RECOMMENDED_PLACE_LIMIT)
                .map(this::toPlaceSummary)
                .toList();
    }

    /**
     * {@code externalPlaceId}는 NAVER 표시용 값이라 {@link PlaceReader} 계약에 없다 —
     * {@code PlaceSearchService}가 내부 canonical 장소에 {@code externalPlaceId=null}을 쓰는 것과
     * 같은 이유로 비워둔다(OpenAPI {@code PlaceSummary.externalPlaceId}는 nullable).
     *
     * <p>{@code externalCategory}(TourAPI 원본 카테고리)도 계약에 없지만 OpenAPI에서 non-null
     * string이라 null로 내릴 수 없다. 원본 값이 없으면 내부 {@code category}의 문자열 값
     * (예: {@code "CAFE"})을 대체값으로 반환한다.
     */
    private PlaceSummary toPlaceSummary(PlaceContract place) {
        return new PlaceSummary(
                place.placeId(),
                null,
                place.name(),
                place.category().name(),
                place.category(),
                place.address(),
                place.latitude(),
                place.longitude(),
                place.saved());
    }
}
