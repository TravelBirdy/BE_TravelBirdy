package com.travelbird.place.domain;

import com.travelbird.common.enums.PlaceCategory;
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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * canonical 장소. Owner = Part 3. (travelbird.dbml places, place.api.PlaceReader)
 *
 * <p>{@code sigunguCode}는 같은 파트가 소유한 {@code sigungu_master}를 가리키는 scalar FK다 —
 * 조회는 {@code region.api.RegionReader}를 통하고, 여기서는 객체 연관관계로 매핑하지 않는다.
 */
@Entity
@Table(name = "places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PlaceStatus status;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "external_category")
    private String externalCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private PlaceCategory category;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "sigungu_code", nullable = false, length = 5)
    private String sigunguCode;

    @Column(name = "latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Place(String name, String description, String externalCategory, PlaceCategory category,
                   String address, String sigunguCode, BigDecimal latitude, BigDecimal longitude,
                   String imageUrl) {
        this.status = PlaceStatus.ACTIVE;
        this.name = name;
        this.description = description;
        this.externalCategory = externalCategory;
        this.category = category;
        this.address = address;
        this.sigunguCode = sigunguCode;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
    }

    /**
     * TourAPI 초기 Import(Seeder)로 canonical 장소를 새로 만들 때 사용한다. 상태는 항상
     * {@link PlaceStatus#ACTIVE}로 시작한다.
     */
    public static Place createFromImport(String name, String description, String externalCategory,
                                          PlaceCategory category, String address, String sigunguCode,
                                          BigDecimal latitude, BigDecimal longitude, String imageUrl) {
        return new Place(name, description, externalCategory, category, address, sigunguCode,
                latitude, longitude, imageUrl);
    }
}
