package com.travelbird.community.domain;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글 공유 클릭 로그. Owner = Part 3. backend-functional-spec-v10.md §3.9.5 — 실제
 * 공유 완료 여부와 무관하게 버튼 클릭마다 한 행씩 남긴다. {@code updated_at} 컬럼이 없어
 * {@code @LastModifiedDate}는 붙이지 않는다.
 */
@Entity
@Table(name = "post_shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PostShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** 비로그인 공유는 {@code null}. */
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 30)
    private ShareChannel channel;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PostShare(Long postId, Long userId, ShareChannel channel) {
        this.postId = postId;
        this.userId = userId;
        this.channel = channel;
    }

    public static PostShare of(Long postId, Long userIdOrNull, ShareChannel channel) {
        return new PostShare(postId, userIdOrNull, channel);
    }
}
