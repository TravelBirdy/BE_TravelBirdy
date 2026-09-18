package com.travelbird.event.service;

import com.travelbird.common.service.RegionReader;
import com.travelbird.event.dto.response.EventListResponse;
import com.travelbird.event.dto.response.EventResponse;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.event.domain.Event;
import com.travelbird.event.domain.EventStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.event.repository.EventRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EventService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final EventRepository eventRepository;
    private final RegionReader regionReader;

    public EventService(EventRepository eventRepository, RegionReader regionReader) {
        this.eventRepository = eventRepository;
        this.regionReader = regionReader;
    }

    public EventListResponse getEvents(LocalDate from, LocalDate to, List<String> sigunguCodes, Long cursor, Integer size) {
        if (sigunguCodes != null && sigunguCodes.isEmpty()) {
            return new EventListResponse(List.of(), null);
        }

        int pageSize = resolveSize(size);
        LocalDate today = LocalDate.now();
        boolean filterBySigungu = sigunguCodes != null;
        List<String> codesParam = filterBySigungu ? sigunguCodes : List.of("");

        List<Event> rows = eventRepository.search(
                today, from, to, filterBySigungu, codesParam, cursor, PageRequest.of(0, pageSize + 1));

        boolean hasMore = rows.size() > pageSize;
        List<Event> page = hasMore ? rows.subList(0, pageSize) : rows;

        Map<String, RegionSummary> regionByCode = regionsByCode(page);
        List<EventResponse> items = page.stream().map(event -> toResponse(event, today, regionByCode)).toList();
        Long nextCursor = hasMore ? page.get(page.size() - 1).getEventId() : null;

        return new EventListResponse(items, nextCursor);
    }

    public EventResponse getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        LocalDate today = LocalDate.now();
        if (event.getEndDate().isBefore(today)) {
            // 종료된 축제는 목록과 마찬가지로 상세에서도 노출하지 않는다 (기능명세서 3.17.1).
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        RegionSummary region = regionReader.getRegion(event.getSigunguCode());
        return toResponse(event, today, Map.of(event.getSigunguCode(), region));
    }

    public List<EventResponse> getMonthlyEvents() {
        LocalDate today = LocalDate.now();
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        return getEvents(today, monthEnd, null, null, MAX_SIZE).items();
    }

    private Map<String, RegionSummary> regionsByCode(List<Event> events) {
        List<String> codes = events.stream().map(Event::getSigunguCode).distinct().toList();
        return regionReader.getRegions(codes).stream()
                .collect(Collectors.toMap(RegionSummary::sigunguCode, region -> region, (a, b) -> a));
    }

    private EventResponse toResponse(Event event, LocalDate today, Map<String, RegionSummary> regionByCode) {
        EventStatus status = event.getStartDate().isAfter(today) ? EventStatus.UPCOMING : EventStatus.ONGOING;
        return new EventResponse(
                event.getEventId(), event.getName(), regionByCode.get(event.getSigunguCode()), event.getPlaceName(),
                event.getStartDate(), event.getEndDate(), event.getThumbnailUrl(), status);
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
