package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * placeId는 Part 3(place 도메인)이 발급한 canonical placeId를 그대로 참조하는 scalar FK다.
 * Place Entity는 Part 3 소유이므로 객체 연관관계로 매핑하지 않는다.
 */
@Entity
@Table(name = "home_recommended_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class HomeRecommendedPlace {

    @Id
    @Column(name = "place_id")
    private Long placeId;

    @Column(name = "display_order", nullable = false, unique = true)
    private Integer displayOrder;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
