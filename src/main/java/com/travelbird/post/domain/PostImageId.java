package com.travelbird.post.domain;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link PostImage}의 복합키. {@code place.domain.SavedPlaceId}와 동일한 팀 컨벤션.
 */
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageId implements Serializable {

    private Long postId;
    private Long fileId;

    public PostImageId(Long postId, Long fileId) {
        this.postId = postId;
        this.fileId = fileId;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getFileId() {
        return fileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostImageId that)) return false;
        return Objects.equals(postId, that.postId) && Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, fileId);
    }
}
