package com.travelbird.event.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    void eventUsesInternalIdAndSeparateTourApiContentId() {
        Event event = new Event(
            "2781234",
            "Seoul Festival",
            "11110",
            "Gwanghwamun Plaza",
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 3),
            "https://example.com/event.webp",
            LocalDateTime.of(2026, 8, 29, 10, 0)
        );

        assertThat(event.getId()).isNull();
        assertThat(event.getTourApiContentId()).isEqualTo("2781234");
    }
}