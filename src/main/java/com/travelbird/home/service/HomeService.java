package com.travelbird.home.service;

import com.travelbird.event.dto.response.EventResponse;
import com.travelbird.event.service.EventService;
import com.travelbird.home.dto.response.HomeResponse;
import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.client.WeatherClient;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final BigDecimal SEOUL_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal SEOUL_LONGITUDE = new BigDecimal("126.9780");

    private final EventService eventService;
    private final WeatherClient weatherClient;

    public HomeService(EventService eventService, WeatherClient weatherClient) {
        this.eventService = eventService;
        this.weatherClient = weatherClient;
    }

    public HomeResponse getHome(BigDecimal latitude, BigDecimal longitude) {
        List<String> unavailableSections = new ArrayList<>();

        boolean hasClientLocation = latitude != null && longitude != null;
        BigDecimal resolvedLatitude = hasClientLocation ? latitude : SEOUL_LATITUDE;
        BigDecimal resolvedLongitude = hasClientLocation ? longitude : SEOUL_LONGITUDE;
        String baseLocation = hasClientLocation ? "현재 위치" : "서울특별시";

        WeatherSummary weather = weatherClient.getWeather(resolvedLatitude, resolvedLongitude, baseLocation);
        if (weather == null) {
            unavailableSections.add("WEATHER");
        }

        List<EventResponse> monthlyEvents = eventService.getMonthlyEvents();

        // Part3 PlaceReader/HomePostReader 실 구현이 이 저장소에 merge되기 전까지는
        // 채울 데이터가 없어 항상 빈 목록 + unavailableSections로 알린다.
        unavailableSections.add("PLACES");
        unavailableSections.add("POSTS");

        return new HomeResponse(weather, List.of(), monthlyEvents, List.of(), unavailableSections);
    }
}
