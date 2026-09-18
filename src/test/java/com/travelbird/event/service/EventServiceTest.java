package com.travelbird.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.region.api.RegionReader;
import com.travelbird.event.domain.Event;
import com.travelbird.event.domain.EventStatus;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.event.repository.EventRepository;
import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private RegionReader regionReader;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, regionReader);
    }

    @Test
    void getEvents_emptySigunguCodes_returnsEmptyWithoutQuerying() {
        var response = eventService.getEvents(LocalDate.now(), LocalDate.now().plusDays(30), List.of(), null, null);

        assertThat(response.items()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        verify(eventRepository, never()).search(any(), any(), any(), anyBoolean(), any(), any(), any());
    }

    @Test
    void getEvents_moreRowsThanSize_returnsNextCursor() {
        Event e1 = newEvent(1L, "축제A", "11110", LocalDate.now(), LocalDate.now().plusDays(1));
        Event e2 = newEvent(2L, "축제B", "11110", LocalDate.now(), LocalDate.now().plusDays(1));
        when(eventRepository.search(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(List.of(e1, e2));
        when(regionReader.getRegions(any())).thenReturn(List.of(new RegionSummary("11110", "종로구")));

        var response = eventService.getEvents(LocalDate.now(), LocalDate.now().plusDays(30), null, null, 1);

        assertThat(response.items()).hasSize(1);
        assertThat(response.nextCursor()).isEqualTo(1L);
    }

    @Test
    void getEvent_notFound_throwsEventNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    void getEvent_alreadyEnded_throwsEventNotFound() {
        Event ended = newEvent(1L, "종료된축제", "11110", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(ended));

        assertThatThrownBy(() -> eventService.getEvent(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    void getEvent_ongoing_returnsOngoingStatus() {
        Event ongoing = newEvent(1L, "축제A", "11110", LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(ongoing));
        when(regionReader.getRegion("11110")).thenReturn(new RegionSummary("11110", "종로구"));

        var response = eventService.getEvent(1L);

        assertThat(response.status()).isEqualTo(EventStatus.ONGOING);
        assertThat(response.region().sigunguName()).isEqualTo("종로구");
    }

    @Test
    void getEvent_upcoming_returnsUpcomingStatus() {
        Event upcoming = newEvent(1L, "축제B", "11110", LocalDate.now().plusDays(5), LocalDate.now().plusDays(7));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(upcoming));
        when(regionReader.getRegion("11110")).thenReturn(new RegionSummary("11110", "종로구"));

        var response = eventService.getEvent(1L);

        assertThat(response.status()).isEqualTo(EventStatus.UPCOMING);
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
