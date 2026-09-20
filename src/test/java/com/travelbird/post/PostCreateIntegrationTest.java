package com.travelbird.post;

import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 작성·임시 저장·발행(`POST /api/posts`) 통합 테스트.
 * backend-functional-spec-v10.md §3.8.1. 다른 도메인 통합 테스트와 동일한
 * Testcontainers+MockMvc+native SQL fixture 패턴.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostCreateIntegrationTest {

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
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private PostPlaceRepository postPlaceRepository;

    @BeforeEach
    void seedFixtures() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID)
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

    private void cancelTrip(Long tripId) {
        entityManager.createNativeQuery("update trips set cancelled_at = now() where trip_id = :id")
                .setParameter("id", tripId)
                .executeUpdate();
        entityManager.clear();
    }

    private Long createTripDayFixture(Long tripId) {
        entityManager.createNativeQuery("insert into trip_days (trip_id, day_number) values (:tripId, 1)")
                .setParameter("tripId", tripId)
                .executeUpdate();
        Number tripDayId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return tripDayId.longValue();
    }

    private Long createPlaceFixture() {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values ('ACTIVE', '테스트 장소', 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)")
                .setParameter("sigunguCode", SIGUNGU_CODE)
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

    /** 트립 하나 + Day 하나 + 그 장소 하나를 세팅하고 placeId를 반환한다. */
    private Long createTripWithPlace(Long tripId) {
        Long tripDayId = createTripDayFixture(tripId);
        Long placeId = createPlaceFixture();
        createTripPlaceFixture(tripId, tripDayId, placeId);
        return placeId;
    }

    private Long createFileFixture(Long ownerUserId, String purpose, String status) {
        entityManager.createNativeQuery(
                        "insert into files (user_id, file_name, object_key, content_type, size_bytes, width, "
                                + "height, purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, "
                                + "'image/jpeg', 1024, 100, 100, :purpose, :status, :expiresAt)")
                .setParameter("userId", ownerUserId)
                .setParameter("objectKey", "post/" + UUID.randomUUID())
                .setParameter("purpose", purpose)
                .setParameter("status", status)
                .setParameter("expiresAt", LocalDateTime.now().plusDays(1))
                .executeUpdate();
        Number fileId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return fileId.longValue();
    }

    private String fileStatus(Long fileId) {
        return (String) entityManager.createNativeQuery("select status from files where file_id = :id")
                .setParameter("id", fileId)
                .getSingleResult();
    }

    private String requestBody(Long tripId, String title, String content, boolean publish) {
        return "{\"tripId\":" + tripId
                + ",\"title\":" + (title == null ? "null" : "\"" + title + "\"")
                + ",\"content\":" + (content == null ? "null" : "\"" + content + "\"")
                + ",\"visibility\":\"PUBLIC\",\"publish\":" + publish + "}";
    }

    @Test
    void 정상_발행하면_201과_경로잠금이_반환된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목", "본문", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.tripRouteLocked").value(true))
                .andExpect(jsonPath("$.publishedAt").exists());
    }

    @Test
    void DRAFT_저장은_제목_본문_없이도_가능하다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, null, null, false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.tripRouteLocked").value(false));
    }

    @Test
    void 발행인데_제목이_없으면_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, null, "본문", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POST_CONTENT"));
    }

    @Test
    void 제목이_50자_초과면_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        authenticateAs(USER_ID);
        String longTitle = "가".repeat(51);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, longTitle, "본문", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_TITLE_TOO_LONG"));
    }

    @Test
    void 동일_트립에_이미_게시글이_있으면_409다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        authenticateAs(USER_ID);
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목", "본문", true)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목2", "본문2", true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_ALREADY_EXISTS_FOR_TRIP"));
    }

    @Test
    void 타인_트립으로_작성하면_403이다() throws Exception {
        Long tripId = createTripFixture(OTHER_USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목", "본문", false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRIP_ACCESS_DENIED"));
    }

    @Test
    void 존재하지_않는_트립은_404다() throws Exception {
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(999_999L, "제목", "본문", false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRIP_NOT_FOUND"));
    }

    @Test
    void 취소된_트립으로_작성하면_409다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        cancelTrip(tripId);
        authenticateAs(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목", "본문", false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRIP_CANCELLED_READ_ONLY"));
    }

    @Test
    void 트립에_속하지_않은_장소는_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long otherTripId = createTripFixture(USER_ID);
        Long placeFromOtherTrip = createTripWithPlace(otherTripId);
        authenticateAs(USER_ID);

        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"placeIds\":[" + placeFromOtherTrip + "],\"visibility\":\"PUBLIC\",\"publish\":false}";

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("POST_PLACE_NOT_IN_TRIP"));
    }

    @Test
    void 트립에_속한_장소는_정상_저장된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithPlace(tripId);
        authenticateAs(USER_ID);

        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"placeIds\":[" + placeId + "],\"visibility\":\"PUBLIC\",\"publish\":false}";

        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));

        assertThat(postPlaceRepository.findByIdPostId(postId)).hasSize(1);
    }

    @Test
    void 타인_소유_이미지는_403이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long fileId = createFileFixture(OTHER_USER_ID, "POST", "UPLOADED");
        authenticateAs(USER_ID);

        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"imageFileIds\":[" + fileId + "],\"visibility\":\"PUBLIC\",\"publish\":false}";

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_ACCESS_DENIED"));
    }

    @Test
    void UPLOADED_상태가_아닌_이미지는_403이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long fileId = createFileFixture(USER_ID, "POST", "PENDING");
        authenticateAs(USER_ID);

        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"imageFileIds\":[" + fileId + "],\"visibility\":\"PUBLIC\",\"publish\":false}";

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FILE_ACCESS_DENIED"));
    }

    @Test
    void 정상_이미지는_저장되고_LINKED로_전환된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long fileId = createFileFixture(USER_ID, "POST", "UPLOADED");
        authenticateAs(USER_ID);

        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"imageFileIds\":[" + fileId + "],\"visibility\":\"PUBLIC\",\"publish\":false}";

        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));

        assertThat(postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(postId)).hasSize(1);
        entityManager.clear();
        assertThat(fileStatus(fileId)).isEqualTo("LINKED");
    }

    @Test
    void 비로그인_작성_요청은_401이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);

        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(tripId, "제목", "본문", false)))
                .andExpect(status().isUnauthorized());
    }
}
