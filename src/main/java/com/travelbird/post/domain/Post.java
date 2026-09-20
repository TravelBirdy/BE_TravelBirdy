package com.travelbird.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 여행 기록(게시글). Owner = Part 3. backend-functional-spec-v10.md §3.8.
 *
 * <p>{@code trip_id}는 Part 2 소유 {@code trips}를 가리키는 scalar FK다 — 이 Entity엔
 * 작성자(user_id) 컬럼이 없고, 소유권은 {@code trip_id -> trips.user_id}로 간접
 * 확인해야 한다(Part2의 {@code TripPostReader}를 통해서만, 직접 조회 금지 —
 * 공통협의 "파트 간 호출의 3원칙").
 *
 * <p>{@code active_trip_id}(MySQL {@code GENERATED ALWAYS AS (...) STORED} 컬럼,
 * {@code deletedAt IS NULL}이면 {@code trip_id} 아니면 {@code NULL})는 의도적으로
 * 매핑하지 않는다 — 앱이 직접 읽거나 쓸 일이 없고, 이 코드베이스에 generated column을
 * 매핑한 선례 자체가 없다. {@code uk_posts_active_trip} 유니크 제약(같은 Trip에 삭제
 * 안 된 Post는 하나만)은 DB가 알아서 강제하며, 위반 시 서비스 계층에서
 * {@code DataIntegrityViolationException}을 잡아 {@code POST_ALREADY_EXISTS_FOR_TRIP}로
 * 변환한다.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "trip_id", nullable = false)
    private Long tripId;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "content")
    private String content;

    @Column(name = "representative_file_id")
    private Long representativeFileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    private PostVisibility visibility;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "save_count", nullable = false)
    private long saveCount;

    @Column(name = "share_count", nullable = false)
    private long shareCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Post(Long tripId, String title, String content, Long representativeFileId,
                 PostVisibility visibility, boolean publish) {
        this.tripId = tripId;
        this.title = title;
        this.content = content;
        this.representativeFileId = representativeFileId;
        this.visibility = visibility;
        this.viewCount = 0;
        this.saveCount = 0;
        this.shareCount = 0;
        if (publish) {
            this.status = PostStatus.PUBLISHED;
            this.publishedAt = LocalDateTime.now();
        } else {
            this.status = PostStatus.DRAFT;
        }
    }

    public static Post create(Long tripId, String title, String content, Long representativeFileId,
                               PostVisibility visibility, boolean publish) {
        return new Post(tripId, title, content, representativeFileId, visibility, publish);
    }

    /**
     * 경로 잠금 판정 기준(공통협의 4.3절 PostRouteLockReader) — {@code publishedAt IS NOT
     * NULL AND deletedAt IS NULL}. visibility가 PRIVATE이거나 status가 BLOCKED여도
     * 삭제되지 않았으면 잠금을 유지한다.
     */
    public boolean isRouteLocked() {
        return publishedAt != null && deletedAt == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateRepresentativeFile(Long fileId) {
        this.representativeFileId = fileId;
    }

    public void changeVisibility(PostVisibility visibility) {
        this.visibility = visibility;
    }

    /** 삭제(tombstone) — 사용자 콘텐츠는 제거하고 참조 무결성을 위해 행은 남긴다. */
    public void tombstone() {
        this.title = null;
        this.content = null;
        this.representativeFileId = null;
        this.deletedAt = LocalDateTime.now();
    }
}
