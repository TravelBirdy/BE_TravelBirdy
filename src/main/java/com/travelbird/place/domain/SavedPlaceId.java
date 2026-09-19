package com.travelbird.place.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link SavedPlace}의 복합키. {@code social.domain.FollowId}와 동일한 팀 컨벤션(
 * {@code @Embeddable} + {@code Serializable}) — {@code @IdClass}는 쓰지 않는다.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SavedPlaceId implements Serializable {

    private Long userId;
    private Long placeId;

    public SavedPlaceId(Long userId, Long placeId) {
        this.userId = userId;
        this.placeId = placeId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getPlaceId() {
        return placeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SavedPlaceId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(placeId, that.placeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, placeId);
    }
}
