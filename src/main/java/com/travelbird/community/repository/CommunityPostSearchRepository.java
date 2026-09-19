package com.travelbird.community.repository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.domain.QPost;
import com.travelbird.post.domain.QPostHashtag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 검색(§3.9.3). 여러 optional 필터 + 다단계 가중치 정렬이 겹쳐서 이 코드베이스
 * 최초로 QueryDSL을 쓴다(다른 도메인은 JPQL/native로 충분해서 안 씀). Spring Data
 * 파생 메서드 조합으로 얻을 게 없어 인터페이스+Impl 관례 대신 구체 클래스로 둔다.
 *
 * <p>가중치 우선순위는 스펙상 4단계(①지역 정확일치 ②태그 정확일치 ③제목 포함 ④본문 포함)지만,
 * ①지역은 {@code TripPostReader}(Part2, 미merge) 없이는 게시글의 지역 자체를 몰라서 이번
 * phase에서는 뺀다(②③④만 반영, TODO로 명시).
 */
@Repository
@RequiredArgsConstructor
public class CommunityPostSearchRepository {

    private final JPAQueryFactory queryFactory;

    private static final QPost post = QPost.post;
    private static final QPostHashtag hashtag = QPostHashtag.postHashtag;

    public List<Post> searchFirstPage(String query, Pageable pageable) {
        return baseQuery(query)
                .orderBy(priority(query).asc(), post.createdAt.desc(), post.postId.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    public List<Post> searchAfterCursor(String query, int cursorPriority, LocalDateTime cursorCreatedAt,
                                         Long cursorPostId, Pageable pageable) {
        NumberExpression<Integer> priority = priority(query);
        BooleanExpression cursorPredicate = priority.gt(cursorPriority)
                .or(priority.eq(cursorPriority).and(post.createdAt.lt(cursorCreatedAt)))
                .or(priority.eq(cursorPriority).and(post.createdAt.eq(cursorCreatedAt)).and(post.postId.lt(cursorPostId)));

        return baseQuery(query)
                .where(cursorPredicate)
                .orderBy(priority.asc(), post.createdAt.desc(), post.postId.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 커서 postId 행의 (priority, createdAt)을 구해 위 페이지 쿼리의 키셋 시드로 쓴다.
     * status/visibility/deletedAt으로 걸러내지 않는다 — Post는 하드 삭제가 없어서
     * (tombstone만) 커서 postId가 한때 실제로 검색 결과에 있었다면 항상 값을 찾을 수 있다.
     * 다만 그 사이 게시글 내용이 바뀌어 더 이상 {@code query}에 매치되지 않으면(엣지 케이스)
     * 빈 값을 반환하고, 호출부는 최하위 우선순위로 취급한다.
     */
    public Optional<Tuple> findCursorSeed(String query, Long cursorPostId) {
        return Optional.ofNullable(
                queryFactory.select(priority(query), post.createdAt)
                        .from(post)
                        .where(post.postId.eq(cursorPostId), matchPredicate(query))
                        .fetchOne());
    }

    private com.querydsl.jpa.impl.JPAQuery<Post> baseQuery(String query) {
        return queryFactory.selectFrom(post)
                .where(post.status.eq(PostStatus.PUBLISHED),
                        post.deletedAt.isNull(),
                        post.visibility.in(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE),
                        matchPredicate(query));
    }

    private BooleanExpression matchPredicate(String query) {
        return tagExact(query).or(titleContains(query)).or(contentContains(query));
    }

    /** ② 태그 정확 일치. */
    private BooleanExpression tagExact(String query) {
        return JPAExpressions.selectOne().from(hashtag)
                .where(hashtag.id.postId.eq(post.postId), hashtag.id.hashtag.eq(query))
                .exists();
    }

    /** ③ 제목 포함(이스케이프된 LIKE). */
    private BooleanExpression titleContains(String query) {
        return post.title.like(likePattern(query), '\\');
    }

    /** ④ 본문 포함(이스케이프된 LIKE). */
    private BooleanExpression contentContains(String query) {
        return post.content.like(likePattern(query), '\\');
    }

    /** ②③④ 순서로 우선순위를 매긴다. ①(지역 정확일치)은 TODO(B1 풀리면 추가). */
    private NumberExpression<Integer> priority(String query) {
        return new CaseBuilder()
                .when(tagExact(query)).then(2)
                .when(titleContains(query)).then(3)
                .otherwise(4);
    }

    /**
     * MySQL LIKE 특수문자(\, %, _)를 이스케이프한다 — QueryDSL {@code .contains()}는
     * 자동 이스케이프하지 않으므로 직접 처리하고 {@code .like(pattern, '\\')}로 사용한다.
     */
    private String likePattern(String query) {
        String escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
