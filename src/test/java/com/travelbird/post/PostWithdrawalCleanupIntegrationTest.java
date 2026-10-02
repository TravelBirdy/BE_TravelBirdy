package com.travelbird.post;

import com.travelbird.ai.api.AiPreviewWithdrawalCleanup;
import com.travelbird.community.domain.PostShare;
import com.travelbird.community.domain.PostViewHistory;
import com.travelbird.community.domain.Report;
import com.travelbird.community.domain.ReportReasonCode;
import com.travelbird.community.domain.ShareChannel;
import com.travelbird.community.repository.PostShareRepository;
import com.travelbird.community.repository.PostViewHistoryRepository;
import com.travelbird.community.repository.ReportRepository;
import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.repository.SavedPlaceRepository;
import com.travelbird.post.api.PostWithdrawalCleanup;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostHashtag;
import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import com.travelbird.trip.api.TripWithdrawalCleanup;
import com.travelbird.user.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 회원 탈퇴 2단계 {@link PostWithdrawalCleanup} 통합 테스트. backend-functional-spec-v10.md
 * §3.1.4. 핵심은 실제 MySQL FK(RESTRICT)다 — 정리 후 Part2 {@link TripWithdrawalCleanup}이
 * Trip까지 지울 수 있어야 하고, 정리를 빼면 FK로 막혀야 한다.
 */
@SpringBootTest
@Transactional
class PostWithdrawalCleanupIntegrationTest {

    private static final String SIGUNGU_CODE = "11110";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long THIRD_USER_ID = 3L;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostWithdrawalCleanup postWithdrawalCleanup;
    @Autowired
    private TripWithdrawalCleanup tripWithdrawalCleanup;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private PostPlaceRepository postPlaceRepository;
    @Autowired
    private PostHashtagRepository postHashtagRepository;
    @Autowired
    private SavedRouteRepository savedRouteRepository;
    @Autowired
    private SavedPlaceRepository savedPlaceRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private PostViewHistoryRepository postViewHistoryRepository;
    @Autowired
    private PostShareRepository postShareRepository;
    @MockitoBean
    private AiPreviewWithdrawalCleanup aiPreviewWithdrawalCleanup;

    @BeforeEach
    void seedUsers() {
        for (Long userId : List.of(USER_ID, OTHER_USER_ID, THIRD_USER_ID)) {
            entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                    .setParameter("id", userId)
                    .executeUpdate();
        }
    }

    private Long insertAndGetId(String sql, Object... namedParams) {
        var query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < namedParams.length; i += 2) {
            query.setParameter((String) namedParams[i], namedParams[i + 1]);
        }
        query.executeUpdate();
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
    }

    private Long createTrip(Long ownerUserId) {
        return insertAndGetId(
                "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                        + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                        + ":start, :end, 'SOLO', 'RELAXED')",
                "userId", ownerUserId, "sigunguCode", SIGUNGU_CODE,
                "start", LocalDate.now(), "end", LocalDate.now().plusDays(1));
    }

    private Long createTripPlace(Long tripId) {
        Long tripDayId = insertAndGetId("insert into trip_days (trip_id, day_number) values (:tripId, 1)",
                "tripId", tripId);
        Long placeId = insertAndGetId(
                "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                        + "values ('ACTIVE', '테스트 장소', 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)",
                "sigunguCode", SIGUNGU_CODE);
        return insertAndGetId(
                "insert into trip_places (trip_id, trip_day_id, place_id, visit_order) "
                        + "values (:tripId, :tripDayId, :placeId, 1)",
                "tripId", tripId, "tripDayId", tripDayId, "placeId", placeId);
    }

    private Long createFile(Long ownerUserId) {
        return insertAndGetId(
                "insert into files (user_id, file_name, object_key, content_type, size_bytes, width, height, "
                        + "purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, 'image/jpeg', "
                        + "1024, 100, 100, 'POST', 'LINKED', :expiresAt)",
                "userId", ownerUserId, "objectKey", "post/" + UUID.randomUUID(),
                "expiresAt", LocalDateTime.now().plusDays(1));
    }

    private Post createPost(Long tripId) {
        return postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, PostVisibility.PUBLIC, true));
    }

    /** 이미지·장소·해시태그가 모두 붙은 발행 Post를 만든다. */
    private Post createFullPost(Long ownerUserId) {
        Long tripId = createTrip(ownerUserId);
        Long tripPlaceId = createTripPlace(tripId);
        Long fileId = createFile(ownerUserId);
        Post post = createPost(tripId);
        postImageRepository.saveAndFlush(PostImage.of(post.getPostId(), fileId, 0));
        postPlaceRepository.saveAndFlush(PostPlace.of(post.getPostId(), tripPlaceId));
        postHashtagRepository.saveAndFlush(PostHashtag.of(post.getPostId(), "바다"));
        return post;
    }

    private long count(String table, String where, Object param) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table + " where " + where)
                .setParameter("p", param).getSingleResult()).longValue();
    }

    private void cleanupAndClear() {
        postWithdrawalCleanup.cleanupUserContent(USER_ID);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 사용자의_모든_Post와_자식_행이_물리_삭제된다() {
        Post published = createFullPost(USER_ID);
        Long tombstonedTripId = createTrip(USER_ID);
        Post tombstoned = createPost(tombstonedTripId);
        tombstoned.tombstone();
        postRepository.saveAndFlush(tombstoned);
        Post othersPost = createFullPost(OTHER_USER_ID);

        cleanupAndClear();

        assertThat(postRepository.findById(published.getPostId())).isEmpty();
        assertThat(postRepository.findById(tombstoned.getPostId())).isEmpty();
        assertThat(count("post_images", "post_id = :p", published.getPostId())).isZero();
        assertThat(count("post_places", "post_id = :p", published.getPostId())).isZero();
        assertThat(count("post_hashtags", "post_id = :p", published.getPostId())).isZero();
        assertThat(postRepository.findById(othersPost.getPostId())).isPresent();
        assertThat(count("post_images", "post_id = :p", othersPost.getPostId())).isEqualTo(1);
    }

    @Test
    void 이미지_파일_행은_Part1_4단계가_지우므로_남겨둔다() {
        Long tripId = createTrip(USER_ID);
        Long fileId = createFile(USER_ID);
        Post post = createPost(tripId);
        postImageRepository.saveAndFlush(PostImage.of(post.getPostId(), fileId, 0));

        cleanupAndClear();

        assertThat(count("files", "file_id = :p", fileId)).isEqualTo(1);
    }

    @Test
    void Post_정리_후_Part2_Trip_삭제가_FK_위반_없이_끝난다() {
        Post post = createFullPost(USER_ID);
        Long tripId = post.getTripId();

        postWithdrawalCleanup.cleanupUserContent(USER_ID);
        tripWithdrawalCleanup.cleanupUserTrips(USER_ID);
        entityManager.flush();
        entityManager.clear();

        assertThat(count("trips", "trip_id = :p", tripId)).isZero();
    }

    @Test
    void Post_정리를_생략하면_Trip_삭제가_FK로_막힌다() {
        createFullPost(USER_ID);
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> {
            tripWithdrawalCleanup.cleanupUserTrips(USER_ID);
            entityManager.flush();
        }).hasStackTraceContaining("Cannot delete or update a parent row");
    }

    @Test
    void 타인의_SavedRoute는_유지되고_접근불가로_표시된다() {
        Post post = createFullPost(USER_ID);
        SavedRoute saved = savedRouteRepository.saveAndFlush(
                SavedRoute.of(OTHER_USER_ID, SavedRouteSourceType.POST, post.getPostId()));

        cleanupAndClear();

        SavedRoute reloaded = savedRouteRepository.findById(saved.getSavedRouteId()).orElseThrow();
        assertThat(reloaded.isSourceAvailable()).isFalse();
    }

    @Test
    void 본인_SavedRoute는_삭제되고_POST_saveCount가_감소하며_AI_Preview는_Part2에_전달된다() {
        Post othersPost = createFullPost(OTHER_USER_ID);
        entityManager.createNativeQuery("update posts set save_count = 1 where post_id = :id")
                .setParameter("id", othersPost.getPostId()).executeUpdate();
        entityManager.clear();
        savedRouteRepository.saveAndFlush(SavedRoute.of(USER_ID, SavedRouteSourceType.POST, othersPost.getPostId()));
        savedRouteRepository.saveAndFlush(SavedRoute.of(USER_ID, SavedRouteSourceType.AI_PREVIEW, 777L));

        cleanupAndClear();

        assertThat(savedRouteRepository.findByUserId(USER_ID)).isEmpty();
        assertThat(postRepository.findById(othersPost.getPostId()).orElseThrow().getSaveCount()).isZero();
        verify(aiPreviewWithdrawalCleanup).demoteUnreferencedPreviews(List.of(777L));
    }

    @Test
    void AI_Preview_저장이_없으면_Part2_강등을_호출하지_않는다() {
        cleanupAndClear();

        verify(aiPreviewWithdrawalCleanup, never()).demoteUnreferencedPreviews(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 본인_부가정보만_삭제되고_타인_데이터는_유지된다() {
        Post othersPost = createFullPost(OTHER_USER_ID);
        Long placeId = ((Number) entityManager.createNativeQuery("select min(place_id) from places").getSingleResult())
                .longValue();
        savedPlaceRepository.saveAndFlush(SavedPlace.of(USER_ID, placeId, "내 메모"));
        savedPlaceRepository.saveAndFlush(SavedPlace.of(OTHER_USER_ID, placeId, "타인 메모"));
        reportRepository.saveAndFlush(Report.create(USER_ID, othersPost.getPostId(), ReportReasonCode.SPAM, null));
        reportRepository.saveAndFlush(Report.create(THIRD_USER_ID, othersPost.getPostId(), ReportReasonCode.SPAM, null));
        postViewHistoryRepository.saveAndFlush(
                PostViewHistory.of(othersPost.getPostId(), USER_ID, LocalDateTime.now()));
        postViewHistoryRepository.saveAndFlush(
                PostViewHistory.of(othersPost.getPostId(), THIRD_USER_ID, LocalDateTime.now()));
        PostShare mine = postShareRepository.saveAndFlush(
                PostShare.of(othersPost.getPostId(), USER_ID, ShareChannel.KAKAO));
        PostShare theirs = postShareRepository.saveAndFlush(
                PostShare.of(othersPost.getPostId(), THIRD_USER_ID, ShareChannel.KAKAO));

        cleanupAndClear();

        assertThat(savedPlaceRepository.findPlaceIdsByUserId(USER_ID)).isEmpty();
        assertThat(savedPlaceRepository.findPlaceIdsByUserId(OTHER_USER_ID)).containsExactly(placeId);
        assertThat(count("reports", "reporter_user_id = :p", USER_ID)).isZero();
        assertThat(count("reports", "reporter_user_id = :p", THIRD_USER_ID)).isEqualTo(1);
        assertThat(count("post_view_histories", "viewer_user_id = :p", USER_ID)).isZero();
        assertThat(count("post_view_histories", "viewer_user_id = :p", THIRD_USER_ID)).isEqualTo(1);
        assertThat(postShareRepository.findById(mine.getId()).orElseThrow().getUserId()).isNull();
        assertThat(postShareRepository.findById(theirs.getId()).orElseThrow().getUserId()).isEqualTo(THIRD_USER_ID);
    }

    @Test
    void 정리해도_호출자가_조회해둔_엔티티는_영속_상태를_유지한다() {
        // WithdrawalService는 정리 호출 전에 User를 조회해 두고 마지막에 user.withdraw()로 상태를 바꾼다.
        // 정리가 영속성 컨텍스트를 비우면 그 변경이 저장되지 않는다.
        entityManager.flush();
        entityManager.clear();
        User user = entityManager.find(User.class, USER_ID);
        createFullPost(USER_ID);
        postShareRepository.saveAndFlush(PostShare.of(
                createFullPost(OTHER_USER_ID).getPostId(), USER_ID, ShareChannel.KAKAO));

        postWithdrawalCleanup.cleanupUserContent(USER_ID);
        user.withdraw();
        entityManager.flush();
        entityManager.clear();

        Object status = entityManager.createNativeQuery("select status from users where user_id = :id")
                .setParameter("id", USER_ID).getSingleResult();
        assertThat(status).isEqualTo("WITHDRAWN");
    }

    @Test
    void 정리할_데이터가_없어도_멱등하게_여러_번_호출된다() {
        postWithdrawalCleanup.cleanupUserContent(USER_ID);
        postWithdrawalCleanup.cleanupUserContent(USER_ID);

        createFullPost(USER_ID);
        cleanupAndClear();
        postWithdrawalCleanup.cleanupUserContent(USER_ID);
    }
}
