package com.travelbird.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.event.client.TourApiClient;
import com.travelbird.event.client.TourApiFestivalResponse;
import com.travelbird.event.domain.Event;
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
class EventSyncSchedulerTest {

    @Mock
    private TourApiClient tourApiClient;
    @Mock
    private SigunguCodeResolver sigunguCodeResolver;
    @Mock
    private EventRepository eventRepository;

    private EventSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EventSyncScheduler(tourApiClient, sigunguCodeResolver, eventRepository);
    }

    @Test
    void syncFestivals_notConfigured_skipsWithoutCallingApi() {
        when(tourApiClient.isConfigured()).thenReturn(false);

        scheduler.syncFestivals();

        verify(tourApiClient, never()).searchFestivals(any(), any(), anyInt(), anyInt());
    }

    @Test
    void syncFestivals_regionUnresolved_skipsItem() {
        when(tourApiClient.isConfigured()).thenReturn(true);
        var item = new TourApiFestivalResponse.Item("C1", "축제", "알수없는주소", null, "20260901", "20260905", null, null);
        when(tourApiClient.searchFestivals(any(), any(), anyInt(), anyInt())).thenReturn(List.of(item), List.of());
        when(sigunguCodeResolver.resolveFromAddress("알수없는주소")).thenReturn(Optional.empty());

        scheduler.syncFestivals();

        verify(eventRepository, never()).save(any());
    }

    @Test
    void syncFestivals_newContentId_createsEvent() {
        when(tourApiClient.isConfigured()).thenReturn(true);
        var item = new TourApiFestivalResponse.Item("C1", "종로축제", "서울특별시 종로구 1", "http://img", "20260901", "20260905", null, null);
        when(tourApiClient.searchFestivals(any(), any(), anyInt(), anyInt())).thenReturn(List.of(item), List.of());
        when(sigunguCodeResolver.resolveFromAddress("서울특별시 종로구 1")).thenReturn(Optional.of("11110"));
        when(eventRepository.findByTourApiContentId("C1")).thenReturn(Optional.empty());

        scheduler.syncFestivals();

        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void syncFestivals_lDongCodePresent_preferredOverAddressMatching() {
        when(tourApiClient.isConfigured()).thenReturn(true);
        var item = new TourApiFestivalResponse.Item(
                "C2", "강남축제", "서울특별시 강남구 1", null, "20260901", "20260905", "11", "680");
        when(tourApiClient.searchFestivals(any(), any(), anyInt(), anyInt())).thenReturn(List.of(item), List.of());
        when(sigunguCodeResolver.exists("11680")).thenReturn(true);
        when(eventRepository.findByTourApiContentId("C2")).thenReturn(Optional.empty());

        scheduler.syncFestivals();

        verify(sigunguCodeResolver, never()).resolveFromAddress(any());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    void syncFestivals_existingContentId_updatesInPlaceWithoutSave() {
        when(tourApiClient.isConfigured()).thenReturn(true);
        var item = new TourApiFestivalResponse.Item("C1", "종로축제(수정)", "서울특별시 종로구 1", null, "20260901", "20260906", null, null);
        when(tourApiClient.searchFestivals(any(), any(), anyInt(), anyInt())).thenReturn(List.of(item), List.of());
        when(sigunguCodeResolver.resolveFromAddress("서울특별시 종로구 1")).thenReturn(Optional.of("11110"));
        Event existing = newEvent("C1", "종로축제", "11110");
        when(eventRepository.findByTourApiContentId("C1")).thenReturn(Optional.of(existing));

        scheduler.syncFestivals();

        verify(eventRepository, never()).save(any());
        assertThat(existing.getName()).isEqualTo("종로축제(수정)");
        assertThat(existing.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 6));
    }

    private Event newEvent(String tourApiContentId, String name, String sigunguCode) {
        try {
            Constructor<Event> constructor = Event.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Event event = constructor.newInstance();
            ReflectionTestUtils.setField(event, "eventId", 1L);
            ReflectionTestUtils.setField(event, "tourApiContentId", tourApiContentId);
            ReflectionTestUtils.setField(event, "name", name);
            ReflectionTestUtils.setField(event, "sigunguCode", sigunguCode);
            ReflectionTestUtils.setField(event, "placeName", name);
            ReflectionTestUtils.setField(event, "startDate", LocalDate.of(2026, 9, 1));
            ReflectionTestUtils.setField(event, "endDate", LocalDate.of(2026, 9, 5));
            return event;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
