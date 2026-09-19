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
 * 게시글 조회 기록. Owner = Part 3. backend-functional-spec-v10.md §3.9.4.
 *
 * <p>이 Entity/Repository는 읽기 전용이다 — INSERT 경로(조회수 증가 API,
 * {@code POST /api/posts/{postId}/views})는 이번 phase 범위 밖이다("작성자 본인 조회
 * 제외" 판정에 {@code TripPostReader}가 필요한데 아직 main에 없다). 지금은 커뮤니티
 * 인기(POPULAR) 탭의 기간별 집계 조회에만 쓴다.
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
}
