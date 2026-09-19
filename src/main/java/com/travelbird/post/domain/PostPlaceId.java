package com.travelbird.post.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link PostPlace}의 복합키. {@code place.domain.SavedPlaceId}와 동일한 팀 컨벤션.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPlaceId implements Serializable {

    private Long postId;
    private Long tripPlaceId;

    public PostPlaceId(Long postId, Long tripPlaceId) {
        this.postId = postId;
        this.tripPlaceId = tripPlaceId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getTripPlaceId() {
        return tripPlaceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostPlaceId that)) return false;
        return Objects.equals(postId, that.postId) && Objects.equals(tripPlaceId, that.tripPlaceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, tripPlaceId);
    }
}
