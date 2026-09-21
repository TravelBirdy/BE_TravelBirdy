package com.travelbird.photomap;

import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 전국 포토맵/지역별 방문 장소 조회 통합 테스트. backend-functional-spec-v10.md §3.11.
 * {@code CommunityDomainIntegrationTest}/{@code SavedRouteDomainIntegrationTest}와 동일한
 * Local MySQL+MockMvc+native SQL fixture 패턴.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PhotoMapDomainIntegrationTest {

    private static final String SIGUNGU_CODE_A = "11110";
    private static final String SIGUNGU_CODE_B = "26440";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostPlaceRepository postPlaceRepository;

    @BeforeEach
    void seedFixtures() {
        insertSigungu(SIGUNGU_CODE_A, "종로구");
        insertSigungu(SIGUNGU_CODE_B, "강서구");
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private void insertSigungu(String code, String name) {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", code)
                .setParameter("name", name)
                .executeUpdate();
    }

    private void insertUser(Long userId) {
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", userId)
                .executeUpdate();
    }

    private Long createTripFixture(Long ownerUserId, String sigunguCode) {
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", ownerUserId)
                .setParameter("sigunguCode", sigunguCode)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        Number tripId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripId.longValue();
    }

    private Long createTripDayFixture(Long tripId) {
        entityManager.createNativeQuery("insert into trip_days (trip_id, day_number) values (:tripId, 1)")
                .setParameter("tripId", tripId)
                .executeUpdate();
        Number tripDayId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripDayId.longValue();
    }

    private Long createPlaceFixture(String sigunguCode) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values ('ACTIVE', '테스트 장소', 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)")
                .setParameter("sigunguCode", sigunguCode)
                .executeUpdate();
        Number placeId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return placeId.longValue();
    }

    private Long createTripPlaceFixture(Long tripId, Long tripDayId, Long placeId) {
        entityManager.createNativeQuery(
                        "insert into trip_places (trip_id, trip_day_id, place_id, visit_order) "
                                + "values (:tripId, :tripDayId, :placeId, 1)")
                .setParameter("tripId", tripId)
                .setParameter("tripDayId", tripDayId)
                .setParameter("placeId", placeId)
                .executeUpdate();
        Number tripPlaceId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripPlaceId.longValue();
    }

    /** 트립 하나 + Day 하나 + 그 장소 하나를 발행 게시글 하나에 건다. */
    private Post publishPostAtPlace(Long ownerUserId, Long placeId, PostVisibility visibility, String sigunguCode) {
        Long tripId = createTripFixture(ownerUserId, sigunguCode);
        Long tripDayId = createTripDayFixture(tripId);
        Long tripPlaceId = createTripPlaceFixture(tripId, tripDayId, placeId);
        Post post = postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, visibility, true));
        postPlaceRepository.save(PostPlace.of(post.getPostId(), tripPlaceId));
        return post;
    }

    private void setPublishedAt(Long postId, LocalDateTime publishedAt) {
        entityManager.createNativeQuery("update posts set published_at = :publishedAt where post_id = :id")
                .setParameter("publishedAt", publishedAt)
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
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

    // ===== /regions =====

    @Test
    void 정상_케이스_지역별_집계가_맞다() throws Exception {
        Long placeA1 = createPlaceFixture(SIGUNGU_CODE_A);
        Long placeA2 = createPlaceFixture(SIGUNGU_CODE_A);
        Long placeB1 = createPlaceFixture(SIGUNGU_CODE_B);
        publishPostAtPlace(USER_ID, placeA1, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeA2, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeB1, PostVisibility.PUBLIC, SIGUNGU_CODE_B);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisitedRegionCount").value(2))
                .andExpect(jsonPath("$.regions.length()").value(2))
                .andExpect(jsonPath("$.regions[0].region.sigunguCode").value(SIGUNGU_CODE_A))
                .andExpect(jsonPath("$.regions[0].visitedPlaceCount").value(2))
                .andExpect(jsonPath("$.regions[0].recordCount").value(2))
                .andExpect(jsonPath("$.regions[1].region.sigunguCode").value(SIGUNGU_CODE_B))
                .andExpect(jsonPath("$.regions[1].visitedPlaceCount").value(1));
    }

    @Test
    void 같은_장소가_여러_게시글에_있어도_방문장소수는_한번만_기록수는_여러번_집계된다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions[0].visitedPlaceCount").value(1))
                .andExpect(jsonPath("$.regions[0].recordCount").value(2));
    }

    @Test
    void DRAFT_게시글은_제외된다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        Long tripId = createTripFixture(USER_ID, SIGUNGU_CODE_A);
        Long tripDayId = createTripDayFixture(tripId);
        Long tripPlaceId = createTripPlaceFixture(tripId, tripDayId, placeId);
        Post draft = postRepository.saveAndFlush(Post.create(tripId, null, null, null, PostVisibility.PRIVATE, false));
        postPlaceRepository.save(PostPlace.of(draft.getPostId(), tripPlaceId));
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(0));
    }

    @Test
    void BLOCKED_게시글은_제외된다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        Post post = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        markBlocked(post.getPostId());
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(0));
    }

    @Test
    void 삭제된_게시글은_제외된다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        Post post = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        markDeleted(post.getPostId());
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(0));
    }

    @Test
    void PRIVATE_공개범위_본인_게시글은_포함된다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeId, PostVisibility.PRIVATE, SIGUNGU_CODE_A);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(1))
                .andExpect(jsonPath("$.regions[0].visitedPlaceCount").value(1));
    }

    @Test
    void 다른_사용자의_게시글은_포함되지_않는다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        publishPostAtPlace(OTHER_USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(0));
    }

    @Test
    void 방문한_장소가_없으면_빈_목록이다() throws Exception {
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(0))
                .andExpect(jsonPath("$.totalVisitedRegionCount").value(0));
    }

    @Test
    void 지역_목록_비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me/photomap/regions"))
                .andExpect(status().isUnauthorized());
    }

    // ===== /regions/{regionCode}/places =====

    @Test
    void 지역별_장소_목록을_조회한다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        Post post = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions/{regionCode}/places", SIGUNGU_CODE_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].placeId").value(placeId))
                .andExpect(jsonPath("$.items[0].name").value("테스트 장소"))
                .andExpect(jsonPath("$.items[0].visitCount").value(1))
                .andExpect(jsonPath("$.items[0].representativePostId").value(post.getPostId()))
                .andExpect(jsonPath("$.items[0].postIds.length()").value(0));
    }

    @Test
    void 같은_장소의_대표게시글과_나머지_postIds가_최신순이다() throws Exception {
        Long placeId = createPlaceFixture(SIGUNGU_CODE_A);
        Post oldest = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        Post middle = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        Post newest = publishPostAtPlace(USER_ID, placeId, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        LocalDateTime base = LocalDateTime.now().minusDays(10);
        setPublishedAt(oldest.getPostId(), base);
        setPublishedAt(middle.getPostId(), base.plusHours(1));
        setPublishedAt(newest.getPostId(), base.plusHours(2));
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions/{regionCode}/places", SIGUNGU_CODE_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].visitCount").value(3))
                .andExpect(jsonPath("$.items[0].representativePostId").value(newest.getPostId()))
                .andExpect(jsonPath("$.items[0].postIds[0]").value(middle.getPostId()))
                .andExpect(jsonPath("$.items[0].postIds[1]").value(oldest.getPostId()));
    }

    @Test
    void 다른_지역_장소는_포함되지_않는다() throws Exception {
        Long placeIdA = createPlaceFixture(SIGUNGU_CODE_A);
        Long placeIdB = createPlaceFixture(SIGUNGU_CODE_B);
        publishPostAtPlace(USER_ID, placeIdA, PostVisibility.PUBLIC, SIGUNGU_CODE_A);
        publishPostAtPlace(USER_ID, placeIdB, PostVisibility.PUBLIC, SIGUNGU_CODE_B);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions/{regionCode}/places", SIGUNGU_CODE_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].placeId").value(placeIdA));
    }

    @Test
    void 존재하지_않는_지역코드는_200과_빈_목록이다() throws Exception {
        // OpenAPI가 이 엔드포인트에 200/401만 정의하고 404가 없어서(oriole0419 PR#16 리뷰),
        // 방문 기록이 없는 지역코드는 검증 없이 그냥 빈 목록으로 응답한다.
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/users/me/photomap/regions/{regionCode}/places", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 장소_목록_비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me/photomap/regions/{regionCode}/places", SIGUNGU_CODE_A))
                .andExpect(status().isUnauthorized());
    }
}
