package com.travelbird.event.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @Column(name = "tour_api_content_id", nullable = false, length = 50, unique = true)
    private String tourApiContentId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sigungu_code", nullable = false, length = 5)
    private String sigunguCode;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Event(String tourApiContentId, String name, String sigunguCode, String placeName, LocalDate startDate, LocalDate endDate, String thumbnailUrl, LocalDateTime updatedAt) {
        this.tourApiContentId = tourApiContentId;
        this.name = name;
        this.sigunguCode = sigunguCode;
        this.placeName = placeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = updatedAt;
    }
}