package com.travelbird.post.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 해시태그. Owner = Part 3. 게시글당 최대 5개, 각 최대 10자
 * (backend-functional-spec-v10.md §3.8.1 — 검증은 {@code PostContentValidator}에서).
 */
@Entity
@Table(name = "post_hashtags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostHashtag {

    @EmbeddedId
    private PostHashtagId id;

    private PostHashtag(PostHashtagId id) {
        this.id = id;
    }

    public static PostHashtag of(Long postId, String hashtag) {
        return new PostHashtag(new PostHashtagId(postId, hashtag));
    }
}
