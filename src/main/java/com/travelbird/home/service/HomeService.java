package com.travelbird.home.service;

import com.travelbird.common.service.RegionReader;
import com.travelbird.event.dto.response.EventResponse;
import com.travelbird.home.dto.response.HomeResponse;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.home.dto.response.WeatherSummary;
import com.travelbird.home.client.WeatherClient;
import com.travelbird.event.domain.Event;
import com.travelbird.event.domain.EventStatus;
import com.travelbird.event.repository.EventRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HomeService {

    private static final BigDecimal SEOUL_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal SEOUL_LONGITUDE = new BigDecimal("126.9780");

    private final EventRepository eventRepository;
    private final RegionReader regionReader;
    private final WeatherClient weatherClient;

    public HomeService(EventRepository eventRepository, RegionReader regionReader, WeatherClient weatherClient) {
        this.eventRepository = eventRepository;
        this.regionReader = regionReader;
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

        List<EventResponse> monthlyEvents = getMonthlyEvents();

        // Part3 PlaceReader/HomePostReader 실 구현이 이 저장소에 merge되기 전까지는
        // 채울 데이터가 없어 항상 빈 목록 + unavailableSections로 알린다.
        unavailableSections.add("PLACES");
        unavailableSections.add("POSTS");

        return new HomeResponse(weather, List.of(), monthlyEvents, List.of(), unavailableSections);
    }

    private List<EventResponse> getMonthlyEvents() {
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<Event> events = eventRepository.findOngoingOrUpcomingWithinMonth(today, monthEnd);

        List<String> sigunguCodes = events.stream().map(Event::getSigunguCode).distinct().toList();
        Map<String, RegionSummary> regionByCode = regionReader.getRegions(sigunguCodes).stream()
                .collect(Collectors.toMap(RegionSummary::sigunguCode, region -> region, (a, b) -> a));

        return events.stream()
                .map(event -> toEventResponse(event, today, regionByCode))
                .toList();
    }

    private EventResponse toEventResponse(Event event, LocalDate today, Map<String, RegionSummary> regionByCode) {
        EventStatus status = event.getStartDate().isAfter(today) ? EventStatus.UPCOMING : EventStatus.ONGOING;
        RegionSummary region = regionByCode.get(event.getSigunguCode());
        return new EventResponse(
                event.getEventId(), event.getName(), region, event.getPlaceName(),
                event.getStartDate(), event.getEndDate(), event.getThumbnailUrl(), status);
    }
}
