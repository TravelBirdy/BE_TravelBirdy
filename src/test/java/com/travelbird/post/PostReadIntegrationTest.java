package com.travelbird.post;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 목록·상세 조회(`GET /api/users/me/posts`, `GET /api/posts/{postId}`) 통합 테스트.
 * backend-functional-spec-v10.md §3.8.3. `PostCreateIntegrationTest`와 동일한
 * Testcontainers+MockMvc+native SQL fixture 패턴 — 실제 `POST /api/posts`를 통해
 * 게시글을 만들어서(직접 insert보다 실제 흐름을 그대로 탄다) place/image까지 채운다.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostReadIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("travelbird")
            .withUsername("travelbird")
            .withPassword("travelbird");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private static final String SIGUNGU_CODE = "11110";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedFixtures() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery(
                        "insert into users (user_id, role, nickname, bird_type) values (:id, 'ROLE_USER', :nickname, :birdType)")
                .setParameter("id", USER_ID)
                .setParameter("nickname", "새길동")
                .setParameter("birdType", "OMOKNUNI")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", OTHER_USER_ID)
                .executeUpdate();
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private Long createTripFixture(Long ownerUserId) {
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", ownerUserId)
                .setParameter("sigunguCode", SIGUNGU_CODE)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        Number tripId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripId.longValue();
    }

    private Long createTripDayFixture(Long tripId, int dayNumber) {
        entityManager.createNativeQuery("insert into trip_days (trip_id, day_number) values (:tripId, :dayNumber)")
                .setParameter("tripId", tripId)
                .setParameter("dayNumber", dayNumber)
                .executeUpdate();
        Number tripDayId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripDayId.longValue();
    }

    private Long createPlaceFixture(String name) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values ('ACTIVE', :name, 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)")
                .setParameter("name", name)
                .setParameter("sigunguCode", SIGUNGU_CODE)
                .executeUpdate();
        Number placeId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return placeId.longValue();
    }

    private Long createTripPlaceFixture(Long tripId, Long tripDayId, Long placeId, int visitOrder, String memo) {
        entityManager.createNativeQuery(
                        "insert into trip_places (trip_id, trip_day_id, place_id, visit_order, memo) "
                                + "values (:tripId, :tripDayId, :placeId, :visitOrder, :memo)")
                .setParameter("tripId", tripId)
                .setParameter("tripDayId", tripDayId)
                .setParameter("placeId", placeId)
                .setParameter("visitOrder", visitOrder)
                .setParameter("memo", memo)
                .executeUpdate();
        Number tripPlaceId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripPlaceId.longValue();
    }

    /** 트립 + Day 1개 + 장소 1개(메모 포함)를 세팅하고 placeId를 반환한다. */
    private Long createTripWithOnePlace(Long tripId, String memo) {
        Long tripDayId = createTripDayFixture(tripId, 1);
        Long placeId = createPlaceFixture("테스트 장소");
        createTripPlaceFixture(tripId, tripDayId, placeId, 1, memo);
        return placeId;
    }

    private Long createPost(Long ownerUserId, Long tripId, Long placeIdOrNull,
                             String visibility, boolean publish) throws Exception {
        authenticateAs(ownerUserId);
        String placesJson = placeIdOrNull == null ? "[]" : "[" + placeIdOrNull + "]";
        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"placeIds\":" + placesJson + ",\"visibility\":\"" + visibility + "\",\"publish\":" + publish + "}";
        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clearAuthentication();
        return Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));
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

    // ===== GET /api/users/me/posts =====

    @Test
    void 내_목록은_공개범위_상태_무관하게_전부_포함한다() throws Exception {
        Long tripId1 = createTripFixture(USER_ID);
        Long tripId2 = createTripFixture(USER_ID);
        Long tripId3 = createTripFixture(USER_ID);
        createPost(USER_ID, tripId1, null, "PRIVATE", true);
        Long blockedPostId = createPost(USER_ID, tripId2, null, "PUBLIC", true);
        markBlocked(blockedPostId);
        createPost(USER_ID, tripId3, null, "PUBLIC", false); // DRAFT

        authenticateAs(USER_ID);
        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void 다른_사용자의_게시글은_내_목록에_안_보인다() throws Exception {
        Long otherTripId = createTripFixture(OTHER_USER_ID);
        createPost(OTHER_USER_ID, otherTripId, null, "PUBLIC", true);

        authenticateAs(USER_ID);
        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 삭제된_게시글은_내_목록에서_제외된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long postId = createPost(USER_ID, tripId, null, "PUBLIC", true);
        markDeleted(postId);

        authenticateAs(USER_ID);
        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 내_목록_커서로_다음_페이지를_가져온다() throws Exception {
        Long tripId1 = createTripFixture(USER_ID);
        Long tripId2 = createTripFixture(USER_ID);
        Long postId1 = createPost(USER_ID, tripId1, null, "PUBLIC", true);
        Long postId2 = createPost(USER_ID, tripId2, null, "PUBLIC", true);

        authenticateAs(USER_ID);
        String first = mockMvc.perform(get("/api/users/me/posts").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].postId").value(postId2))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn().getResponse().getContentAsString();

        Long nextCursor = Long.valueOf(first.split("\"nextCursor\":")[1].replaceAll("[^0-9].*", ""));

        mockMvc.perform(get("/api/users/me/posts").param("size", "1").param("cursor", nextCursor.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].postId").value(postId1))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void 내_목록_비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isUnauthorized());
    }

    // ===== GET /api/posts/{postId} =====

    @Test
    void 비로그인이_공개_게시글_상세를_조회할_수_있다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithOnePlace(tripId, "좋았던 장소");
        Long postId = createPost(USER_ID, tripId, placeId, "PUBLIC", true);

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제목"))
                .andExpect(jsonPath("$.author.userId").value(USER_ID))
                .andExpect(jsonPath("$.author.nickname").value("새길동"))
                .andExpect(jsonPath("$.author.birdType").value("OMOKNUNI"))
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].placeId").value(placeId))
                .andExpect(jsonPath("$.places[0].memo").value("좋았던 장소"))
                .andExpect(jsonPath("$.places[0].memoMasked").value(false))
                .andExpect(jsonPath("$.route.length()").value(1))
                .andExpect(jsonPath("$.route[0].dayNumber").value(1))
                .andExpect(jsonPath("$.route[0].routePoints.length()").value(1));
    }

    @Test
    void 비로그인이_비공개_게시글을_조회하면_404다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long postId = createPost(USER_ID, tripId, null, "PRIVATE", true);

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 비로그인이_차단된_게시글을_조회하면_404다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long postId = createPost(USER_ID, tripId, null, "PUBLIC", true);
        markBlocked(postId);

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 작성자_본인은_비공개_DRAFT_게시글도_조회할_수_있다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long postId = createPost(USER_ID, tripId, null, "PRIVATE", false);

        authenticateAs(USER_ID);
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
    }

    @Test
    void MEMO_PRIVATE_비작성자는_장소_메모는_가려지고_사진은_보인다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithOnePlace(tripId, "비밀 메모");
        Long postId = createPost(USER_ID, tripId, placeId, "MEMO_PRIVATE", true);

        authenticateAs(OTHER_USER_ID);
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].memo").doesNotExist())
                .andExpect(jsonPath("$.places[0].memoMasked").value(true))
                .andExpect(jsonPath("$.places[0].placeId").value(placeId));
    }

    @Test
    void MEMO_PRIVATE_작성자_본인은_메모가_그대로_보인다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithOnePlace(tripId, "비밀 메모");
        Long postId = createPost(USER_ID, tripId, placeId, "MEMO_PRIVATE", true);

        authenticateAs(USER_ID);
        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places[0].memo").value("비밀 메모"))
                .andExpect(jsonPath("$.places[0].memoMasked").value(false));
    }

    @Test
    void 존재하지_않는_게시글_상세는_404다() throws Exception {
        mockMvc.perform(get("/api/posts/{postId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 여러_Day에_걸친_route가_Day와_방문순서대로_정렬된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long day1 = createTripDayFixture(tripId, 1);
        Long day2 = createTripDayFixture(tripId, 2);
        Long placeA = createPlaceFixture("장소A");
        Long placeB = createPlaceFixture("장소B");
        Long placeC = createPlaceFixture("장소C");
        createTripPlaceFixture(tripId, day1, placeA, 2, null);
        createTripPlaceFixture(tripId, day1, placeB, 1, null);
        createTripPlaceFixture(tripId, day2, placeC, 1, null);

        authenticateAs(USER_ID);
        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"placeIds\":[" + placeA + "," + placeB + "," + placeC + "],"
                + "\"visibility\":\"PUBLIC\",\"publish\":true}";
        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));
        clearAuthentication();

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.length()").value(2))
                .andExpect(jsonPath("$.route[0].dayNumber").value(1))
                .andExpect(jsonPath("$.route[0].routePoints.length()").value(2))
                .andExpect(jsonPath("$.route[1].dayNumber").value(2))
                .andExpect(jsonPath("$.places[0].placeId").value(placeB)) // day1, visitOrder1이 먼저
                .andExpect(jsonPath("$.places[1].placeId").value(placeA))
                .andExpect(jsonPath("$.places[2].placeId").value(placeC));
    }
}
