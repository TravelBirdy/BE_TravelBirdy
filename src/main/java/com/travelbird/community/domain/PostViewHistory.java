package com.travelbird.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 게시글 조회 기록. Owner = Part 3. backend-functional-spec-v10.md §3.9.4 — 로그인
 * 사용자의 게시글 상세 조회를 사용자당 게시글별 24시간에 한 번만 집계한다(중복 판정은
 * {@code post.service.PostViewService} 참고).
 */
@Entity
@Table(name = "post_view_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_history_id")
    private Long viewHistoryId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "viewer_user_id", nullable = false)
    private Long viewerUserId;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    private PostViewHistory(Long postId, Long viewerUserId, LocalDateTime viewedAt) {
        this.postId = postId;
        this.viewerUserId = viewerUserId;
        this.viewedAt = viewedAt;
    }

    public static PostViewHistory of(Long postId, Long viewerUserId, LocalDateTime viewedAt) {
        return new PostViewHistory(postId, viewerUserId, viewedAt);
    }
}
