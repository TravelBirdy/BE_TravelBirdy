package com.travelbird.community.repository;

import com.travelbird.post.domain.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 인기(POPULAR) 탭 기간별 점수 조회. backend-functional-spec-v10.md §3.9.2 —
 * 점수 = 기간 내 조회수×1 + 기간 내 공유수×5(저장수는 SavedRoute 도메인 미구현으로 항상
 * 0, {@code post.service.HomePostReaderImpl}과 동일).
 *
 * <p>{@code post_view_histories}/{@code post_shares}(Community 소유 테이블)를 파생
 * 테이블로 조인해야 해서 JPQL로 표현하기 어색하다 — {@code PostRepository}(Post 전용)를
 * Community 테이블에 얽히게 하지 않으려고 이 Repository로 분리하고 native SQL을 쓴다.
 * CRUD가 필요 없어 {@code Repository<Post, Long>}(마커 인터페이스)만 확장한다.
 */
public interface CommunityPopularFeedRepository extends Repository<Post, Long> {

    /**
     * 커서(없으면 첫 페이지) 이후 {@code (score, publishedAt, postId)} 내림차순으로
     * postId 목록을 반환한다. {@code cursorScoreOrNull}이 {@code null}이면 커서 없음으로
     * 취급한다. {@code excludedTripIds}는 차단 관계 tripId 목록 — 제외할 게 없으면
     * {@code List.of(-1L)}처럼 존재할 수 없는 값을 넘긴다(빈 {@code IN()}은 SQL 오류).
     */
    @Query(value = """
            select post_id from (
              select p.post_id as post_id, p.published_at as published_at,
                     (coalesce(v.cnt, 0) * 1 + coalesce(s.cnt, 0) * 5) as score
              from posts p
              left join (select post_id, count(*) as cnt from post_view_histories
                         where viewed_at >= :from and viewed_at < :to group by post_id) v on v.post_id = p.post_id
              left join (select post_id, count(*) as cnt from post_shares
                         where created_at >= :from and created_at < :to group by post_id) s on s.post_id = p.post_id
              where p.status = 'PUBLISHED' and p.deleted_at is null
                and p.visibility in ('PUBLIC', 'MEMO_PRIVATE')
                and p.trip_id not in (:excludedTripIds)
            ) scored
            where (:cursorScore is null)
               or (score < :cursorScore)
               or (score = :cursorScore and published_at < :cursorPublishedAt)
               or (score = :cursorScore and published_at = :cursorPublishedAt and post_id < :cursorPostId)
            order by score desc, published_at desc, post_id desc
            """, nativeQuery = true)
    List<Long> findPopularFeedPostIds(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("excludedTripIds") List<Long> excludedTripIds,
                                       @Param("cursorScore") Long cursorScoreOrNull,
                                       @Param("cursorPublishedAt") LocalDateTime cursorPublishedAtOrNull,
                                       @Param("cursorPostId") Long cursorPostIdOrNull,
                                       Pageable pageable);

    /**
     * 커서로 받은 postId 자신의 (score, publishedAt)을 구해 위 쿼리의 키셋 시드로 쓴다.
     * 의도적으로 status/visibility/deletedAt으로 걸러내지 않는다 — Post는 하드 삭제가
     * 없고(tombstone만) BLOCKED/PRIVATE로 바뀌어도 행 자체는 남기 때문에, 커서 postId가
     * 한때 실제로 존재했다면 이 조회는 항상 값을 찾아야 한다(§10 커서 정책 참고).
     */
    @Query(value = """
            select (coalesce(v.cnt,0)*1 + coalesce(s.cnt,0)*5) as score, p.published_at as publishedAt
            from posts p
            left join (select post_id, count(*) as cnt from post_view_histories
                       where viewed_at >= :from and viewed_at < :to group by post_id) v on v.post_id = p.post_id
            left join (select post_id, count(*) as cnt from post_shares
                       where created_at >= :from and created_at < :to group by post_id) s on s.post_id = p.post_id
            where p.post_id = :postId
            """, nativeQuery = true)
    Optional<PopularCursorSeed> findScoreAndPublishedAtByPostId(@Param("postId") Long postId,
                                                                 @Param("from") LocalDateTime from,
                                                                 @Param("to") LocalDateTime to);

    interface PopularCursorSeed {
        Long getScore();

        LocalDateTime getPublishedAt();
    }
}
