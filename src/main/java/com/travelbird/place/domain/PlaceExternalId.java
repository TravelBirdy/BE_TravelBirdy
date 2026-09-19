package com.travelbird.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 외부 Provider(TourAPI/NAVER) 장소 ID와 canonical {@code placeId}의 매핑. Owner = Part 3.
 * {@code (provider, externalPlaceId)} 조합이 Unique — 동일 외부 ID의 재수입은 이 조합으로
 * 멱등하게 처리한다 (place.api.PlaceReader, backend-functional-spec-v10.md §3.6.5).
 *
 * <p>{@code placeId}는 같은 파트가 소유한 {@link Place}를 가리키는 scalar FK다 — 양방향
 * 연관관계가 필요한 소비자가 아직 없어 객체 그래프로 매핑하지 않는다.
 */
@Entity
@Table(name = "place_external_ids")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PlaceExternalId {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PlaceExternalIdProvider provider;

    @Column(name = "external_place_id", nullable = false)
    private String externalPlaceId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PlaceExternalId(Long placeId, PlaceExternalIdProvider provider, String externalPlaceId) {
        this.placeId = placeId;
        this.provider = provider;
        this.externalPlaceId = externalPlaceId;
    }

    public static PlaceExternalId of(Long placeId, PlaceExternalIdProvider provider, String externalPlaceId) {
        return new PlaceExternalId(placeId, provider, externalPlaceId);
    }
}
