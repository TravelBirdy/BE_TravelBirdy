package com.travelbird.post.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link PostHashtag}의 복합키. {@code place.domain.SavedPlaceId}와 동일한 팀 컨벤션.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostHashtagId implements Serializable {

    private Long postId;
    private String hashtag;

    public PostHashtagId(Long postId, String hashtag) {
        this.postId = postId;
        this.hashtag = hashtag;
    }

    public Long getPostId() {
        return postId;
    }

    public String getHashtag() {
        return hashtag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostHashtagId that)) return false;
        return Objects.equals(postId, that.postId) && Objects.equals(hashtag, that.hashtag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, hashtag);
    }
}
