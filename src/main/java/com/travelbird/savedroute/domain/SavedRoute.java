package com.travelbird.savedroute.domain;

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

import java.time.LocalDateTime;

/**
 * 저장한 경로. Owner = Part 3. backend-functional-spec-v10.md §3.10.1.
 *
 * <p>{@code sourceId}는 {@code sourceType}에 따라 {@code posts.post_id} 또는
 * {@code ai_trip_previews.preview_id}를 가리키는 polymorphic scalar 참조다 — FK를 걸지
 * 않는다({@code SavedPlace}와 달리 이 테이블은 원래부터 그렇게 설계됨, 공통협의 4.3절).
 * 멱등성은 컴포짓 PK가 아니라 {@code uk_saved_route_source(user_id, source_type, source_id)}
 * 유니크 제약으로 확보하므로, PK는 단순 surrogate {@code saved_route_id}다.
 *
 * <p>{@code savedAt}은 최초 저장 시각을 유지해야 해서(재수정 시에도 불변) {@code @CreatedDate}로
 * 관리한다 — Post의 {@code createdAt}과 동일한 이유(Post.java 참고)로 Hibernate 1차 캐시에는
 * 반올림 전 값이 남으므로, 같은 트랜잭션 안에서 커서로 재사용할 때는 반올림 처리가 필요하다
 * ({@code CommunityFeedService.roundToStoredPrecision}과 동일 이슈).
 */
@Entity
@Table(name = "saved_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SavedRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "saved_route_id")
    private Long savedRouteId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SavedRouteSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_available", nullable = false)
    private boolean sourceAvailable;

    @CreatedDate
    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SavedRoute(Long userId, SavedRouteSourceType sourceType, Long sourceId) {
        this.userId = userId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.sourceAvailable = true;
    }

    public static SavedRoute of(Long userId, SavedRouteSourceType sourceType, Long sourceId) {
        return new SavedRoute(userId, sourceType, sourceId);
    }

    public void markAvailable() {
        this.sourceAvailable = true;
    }

    public void markUnavailable() {
        this.sourceAvailable = false;
    }
}
