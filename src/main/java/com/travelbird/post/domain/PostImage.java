package com.travelbird.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 첨부 이미지. Owner = Part 3. {@code fileId}는 Part 1 소유({@code files})를
 * 가리키는 scalar FK다. {@code file_id}는 DB에서 전체 게시글을 통틀어 유니크하다
 * (한 파일은 하나의 게시글에만 첨부될 수 있음, {@code uk_post_image_file}).
 */
@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage {

    @EmbeddedId
    private PostImageId id;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private PostImage(PostImageId id, int displayOrder) {
        this.id = id;
        this.displayOrder = displayOrder;
    }

    public static PostImage of(Long postId, Long fileId, int displayOrder) {
        return new PostImage(new PostImageId(postId, fileId), displayOrder);
    }
}
