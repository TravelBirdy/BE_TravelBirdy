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

    /**
     * 커뮤니티 ALL 탭 첫 페이지. {@code publishedAt DESC, postId DESC} 순.
     *
     * @param excludedTripIds 차단 관계인 유저들의 tripId 목록(양방향). 제외할 게 없으면
     *                        절대 존재할 수 없는 값(예: {@code List.of(-1L)})을 넘긴다 —
     *                        JPQL {@code NOT IN ()}에 빈 컬렉션을 못 쓰기 때문의 관례적 처리.
     */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "and p.tripId not in :excludedTripIds "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findAllFeedFirstPage(@Param("status") PostStatus status,
                                     @Param("visibilities") List<PostVisibility> visibilities,
                                     @Param("excludedTripIds") List<Long> excludedTripIds,
                                     Pageable pageable);

    /** 커뮤니티 ALL 탭 커서 이후 페이지. {@code excludedTripIds}는 위와 동일. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "and p.tripId not in :excludedTripIds "
            + "and (p.publishedAt < :cursorPublishedAt "
            + "or (p.publishedAt = :cursorPublishedAt and p.postId < :cursorPostId)) "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findAllFeedAfterCursor(@Param("status") PostStatus status,
                                       @Param("visibilities") List<PostVisibility> visibilities,
                                       @Param("excludedTripIds") List<Long> excludedTripIds,
                                       @Param("cursorPublishedAt") LocalDateTime cursorPublishedAt,
                                       @Param("cursorPostId") Long cursorPostId,
                                       Pageable pageable);

    /**
     * 커뮤니티 FOLLOWING 탭 첫 페이지. {@code includedTripIds}는 팔로우 중인 유저들의
     * tripId 목록 — 서비스 레이어에서 비어있으면 이 쿼리 자체를 호출하지 않고 빈 목록을
     * 바로 반환한다(팔로우가 0명이면 JPQL {@code IN ()} 빈 컬렉션 문제와 무관하게 자명함).
     */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "and p.tripId in :includedTripIds and p.tripId not in :excludedTripIds "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findFollowingFeedFirstPage(@Param("status") PostStatus status,
                                           @Param("visibilities") List<PostVisibility> visibilities,
                                           @Param("includedTripIds") List<Long> includedTripIds,
                                           @Param("excludedTripIds") List<Long> excludedTripIds,
                                           Pageable pageable);

    /** 커뮤니티 FOLLOWING 탭 커서 이후 페이지. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "and p.tripId in :includedTripIds and p.tripId not in :excludedTripIds "
            + "and (p.publishedAt < :cursorPublishedAt "
            + "or (p.publishedAt = :cursorPublishedAt and p.postId < :cursorPostId)) "
            + "order by p.publishedAt desc, p.postId desc")
    List<Post> findFollowingFeedAfterCursor(@Param("status") PostStatus status,
                                             @Param("visibilities") List<PostVisibility> visibilities,
                                             @Param("includedTripIds") List<Long> includedTripIds,
                                             @Param("excludedTripIds") List<Long> excludedTripIds,
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

    /** 조회수 증가도 공유수와 동일한 이유로 벌크 업데이트로 처리한다(낙관적 락 우회). */
    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.postId = :postId and p.deletedAt is null")
    int incrementViewCount(@Param("postId") Long postId);

    /** 경로 저장 시 saveCount 증가(낙관적 락 우회) — backend-functional-spec-v10.md §3.10.1. */
    @Modifying
    @Query("update Post p set p.saveCount = p.saveCount + 1 where p.postId = :postId")
    int incrementSaveCount(@Param("postId") Long postId);

    /** 경로 저장 취소 시 saveCount 감소. 0 미만으로 내려가지 않도록 방어한다. */
    @Modifying
    @Query("update Post p set p.saveCount = p.saveCount - 1 where p.postId = :postId and p.saveCount > 0")
    int decrementSaveCount(@Param("postId") Long postId);
}
