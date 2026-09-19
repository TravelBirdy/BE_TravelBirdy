package com.travelbird.post.repository;

import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByTripIdAndDeletedAtIsNull(Long tripId);

    Optional<Post> findByTripIdAndDeletedAtIsNull(Long tripId);

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);

    boolean existsByPostIdAndDeletedAtIsNull(Long postId);

    /** 비작성자 접근 가능 여부 판정(상세/공유 등에서 공통으로 쓰는 unified 404 패턴). */
    Optional<Post> findByPostIdAndStatusAndVisibilityInAndDeletedAtIsNull(
            Long postId, PostStatus status, List<PostVisibility> visibilities);

    /** 인기 점수(조회수×1+저장수×3+공유수×5) 내림차순 후보군. {@code HomePostReaderImpl}가 셔플 전 pool로 사용. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "order by (p.viewCount + p.saveCount * 3 + p.shareCount * 5) desc")
    List<Post> findHomeCandidates(@Param("status") PostStatus status,
                                   @Param("visibilities") List<PostVisibility> visibilities,
                                   Pageable pageable);

    /** 커뮤니티 ALL 탭 첫 페이지. {@code publishedAt DESC, postId DESC} 순. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findAllFeedFirstPage(@Param("status") PostStatus status,
                                     @Param("visibilities") List<PostVisibility> visibilities,
                                     Pageable pageable);

    /** 커뮤니티 ALL 탭 커서 이후 페이지. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "and (p.publishedAt < :cursorPublishedAt "
            + "or (p.publishedAt = :cursorPublishedAt and p.postId < :cursorPostId)) "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findAllFeedAfterCursor(@Param("status") PostStatus status,
                                       @Param("visibilities") List<PostVisibility> visibilities,
                                       @Param("cursorPublishedAt") LocalDateTime cursorPublishedAt,
                                       @Param("cursorPostId") Long cursorPostId,
                                       Pageable pageable);

    /**
     * 공유수 증가는 일반 {@code save()}(dirty-checking)가 아니라 벌크 업데이트로 처리한다 —
     * {@code Post}는 {@code @Version} 낙관적 락을 쓰는데, 인기 게시글에 동시 공유 클릭이
     * 몰릴 때마다 버전 충돌로 {@code 409}가 나는 건 카운터 증가 API로서 말이 안 된다.
     * 벌크 쿼리는 버전 검사를 거치지 않는다.
     */
    @Modifying
    @Query("update Post p set p.shareCount = p.shareCount + 1 where p.postId = :postId and p.deletedAt is null")
    int incrementShareCount(@Param("postId") Long postId);
}
