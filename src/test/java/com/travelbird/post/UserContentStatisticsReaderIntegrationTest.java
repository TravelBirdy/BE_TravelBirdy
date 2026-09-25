package com.travelbird.post;

import com.travelbird.photomap.service.PhotoMapService;
import com.travelbird.post.api.UserContentStatistics;
import com.travelbird.post.api.UserContentStatisticsReader;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.post.service.UserContentStatisticsReaderImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 마이페이지 집계용 {@link UserContentStatisticsReader} 실구현 통합 테스트.
 * backend-functional-spec-v10.md §3.14.1 — 기록 수는 삭제·DRAFT·BLOCKED 제외(PRIVATE 포함),
 * 방문 지역 수는 포토맵과 동일한 5자리 시군구 기준.
 */
@SpringBootTest
@Transactional
class UserContentStatisticsReaderIntegrationTest {

    private static final String SIGUNGU_CODE_A = "11110";
    private static final String SIGUNGU_CODE_B = "26440";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private UserContentStatisticsReader reader;
    @Autowired
    private PhotoMapService photoMapService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostPlaceRepository postPlaceRepository;

    @BeforeEach
    void seedUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    private void insertUser(Long userId) {
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", userId)
                .executeUpdate();
    }

    private Long lastInsertId() {
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
    }

    private Long createPlaceFixture(String sigunguCode) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values ('ACTIVE', '테스트 장소', 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)")
                .setParameter("sigunguCode", sigunguCode)
                .executeUpdate();
        return lastInsertId();
    }

    /** 트립 + Day + 장소 하나를 만들고 게시글 하나(place 연결 포함)를 저장한다. */
    private Post createPost(Long ownerUserId, String sigunguCode, PostVisibility visibility, boolean publish) {
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", ownerUserId)
                .setParameter("sigunguCode", sigunguCode)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        Long tripId = lastInsertId();
        entityManager.createNativeQuery("insert into trip_days (trip_id, day_number) values (:tripId, 1)")
                .setParameter("tripId", tripId)
                .executeUpdate();
        Long tripDayId = lastInsertId();
        Long placeId = createPlaceFixture(sigunguCode);
        entityManager.createNativeQuery(
                        "insert into trip_places (trip_id, trip_day_id, place_id, visit_order) "
                                + "values (:tripId, :tripDayId, :placeId, 1)")
                .setParameter("tripId", tripId)
                .setParameter("tripDayId", tripDayId)
                .setParameter("placeId", placeId)
                .executeUpdate();
        Long tripPlaceId = lastInsertId();
        Post post = postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, visibility, publish));
        postPlaceRepository.save(PostPlace.of(post.getPostId(), tripPlaceId));
        return post;
    }

    private void markBlocked(Long postId) {
        entityManager.createNativeQuery("update posts set status = 'BLOCKED' where post_id = :id")
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
    }

    private void markDeleted(Long postId) {
        entityManager.createNativeQuery("update posts set deleted_at = now() where post_id = :id")
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
    }

    @Test
    void Part1의_NoOp_대신_실구현_Bean이_등록된다() {
        assertThat(reader).isInstanceOf(UserContentStatisticsReaderImpl.class);
    }

    @Test
    void 게시글이_없으면_0과_0이다() {
        assertThat(reader.getStatistics(USER_ID)).isEqualTo(new UserContentStatistics(0, 0));
    }

    @Test
    void 기록_수는_DRAFT_BLOCKED_삭제를_제외하고_PRIVATE은_포함한다() {
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PRIVATE, true);
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, false); // DRAFT
        Post blocked = createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        markBlocked(blocked.getPostId());
        Post deleted = createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        markDeleted(deleted.getPostId());

        assertThat(reader.getStatistics(USER_ID).postCount()).isEqualTo(2);
    }

    @Test
    void 다른_사용자의_게시글은_집계에_포함되지_않는다() {
        createPost(OTHER_USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);

        assertThat(reader.getStatistics(USER_ID)).isEqualTo(new UserContentStatistics(0, 0));
    }

    @Test
    void 방문_지역_수는_5자리_시군구_기준으로_중복없이_센다() {
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PRIVATE, true); // 같은 지역은 한 번만
        createPost(USER_ID, SIGUNGU_CODE_B, PostVisibility.PUBLIC, true);

        assertThat(reader.getStatistics(USER_ID).visitedRegionCount()).isEqualTo(2);
    }

    @Test
    void DRAFT나_BLOCKED_게시글의_지역은_방문_지역에_포함되지_않는다() {
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        createPost(USER_ID, SIGUNGU_CODE_B, PostVisibility.PUBLIC, false); // DRAFT
        Post blocked = createPost(USER_ID, "11140", PostVisibility.PUBLIC, true);
        markBlocked(blocked.getPostId());

        assertThat(reader.getStatistics(USER_ID).visitedRegionCount()).isEqualTo(1);
    }

    @Test
    void 방문_지역_수는_포토맵의_totalVisitedRegionCount와_항상_같다() {
        createPost(USER_ID, SIGUNGU_CODE_A, PostVisibility.PUBLIC, true);
        createPost(USER_ID, SIGUNGU_CODE_B, PostVisibility.PRIVATE, true);
        createPost(USER_ID, "11140", PostVisibility.PUBLIC, false); // DRAFT

        assertThat(reader.getStatistics(USER_ID).visitedRegionCount())
                .isEqualTo(photoMapService.getRegions(USER_ID).totalVisitedRegionCount());
    }
}
