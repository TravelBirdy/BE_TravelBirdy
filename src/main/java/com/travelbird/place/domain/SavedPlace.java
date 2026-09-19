package com.travelbird.place.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자의 장소 저장. Owner = Part 3. {@code (user_id, place_id)} 복합키라서 {@code userId}
 * 저장은 멱등하다 (backend-functional-spec-v10.md §3.6.3).
 *
 * <p>{@code userId}는 Part 1 소유(User)라 scalar로 두고, {@code placeId}도 대칭성과
 * 단순함을 위해 scalar로 둔다 — 장소 정보는 이 엔티티가 아니라 {@code place.api.PlaceReader}로
 * 조회한다.
 */
@Entity
@Table(name = "saved_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class SavedPlace {

    @EmbeddedId
    private SavedPlaceId id;

    @Column(name = "memo", length = 100)
    private String memo;

    @CreatedDate
    @Column(name = "saved_at", nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SavedPlace(SavedPlaceId id, String memo) {
        this.id = id;
        this.memo = memo;
    }

    public static SavedPlace of(Long userId, Long placeId, String memo) {
        return new SavedPlace(new SavedPlaceId(userId, placeId), memo);
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
