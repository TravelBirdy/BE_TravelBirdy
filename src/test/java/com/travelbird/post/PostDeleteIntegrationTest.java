package com.travelbird.post;

import com.travelbird.file.client.S3FileStorage;
import com.travelbird.photomap.service.PhotoMapService;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 삭제(`DELETE /api/posts/{postId}`) 통합 테스트. backend-functional-spec-v10.md §3.8.4.
 * `addFilters=false`로 인증 fixture 편의를 취하고, 실제 필터 체인의 비로그인 401은
 * {@link PostDetailSecurityFilterIntegrationTest}와 같은 방식으로 별도 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostDeleteIntegrationTest {

    private static final String SIGUNGU_CODE = "11110";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private PostPlaceRepository postPlaceRepository;
    @Autowired
    private SavedRouteRepository savedRouteRepository;
    @Autowired
    private PhotoMapService photoMapService;
    @MockitoBean
    private S3FileStorage s3FileStorage;

    @BeforeEach
    void seedUsers() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void insertUser(Long userId) {
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", userId)
                .executeUpdate();
    }

    private void authenticateAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }

    private Long lastInsertId() {
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
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
        return lastInsertId();
    }

    private Long createTripDayFixture(Long tripId) {
        entityManager.createNativeQuery("insert into trip_days (trip_id, day_number) values (:tripId, 1)")
                .setParameter("tripId", tripId)
                .executeUpdate();
        return lastInsertId();
    }

    private Long createPlaceFixture() {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values ('ACTIVE', '테스트 장소', 'ATTRACTION', '테스트 주소', :sigunguCode, 37.5, 127.0)")
                .setParameter("sigunguCode", SIGUNGU_CODE)
                .executeUpdate();
        return lastInsertId();
    }

    private Long createTripPlaceFixture(Long tripId, Long tripDayId, Long placeId) {
        entityManager.createNativeQuery(
                        "insert into trip_places (trip_id, trip_day_id, place_id, visit_order) "
                                + "values (:tripId, :tripDayId, :placeId, 1)")
                .setParameter("tripId", tripId)
                .setParameter("tripDayId", tripDayId)
                .setParameter("placeId", placeId)
                .executeUpdate();
        return lastInsertId();
    }

    private Long createFileFixture(Long ownerUserId) {
        entityManager.createNativeQuery(
                        "insert into files (user_id, file_name, object_key, content_type, size_bytes, width, "
                                + "height, purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, "
                                + "'image/jpeg', 1024, 100, 100, 'POST', 'UPLOADED', :expiresAt)")
                .setParameter("userId", ownerUserId)
                .setParameter("objectKey", "post/" + UUID.randomUUID())
                .setParameter("expiresAt", LocalDateTime.now().plusDays(1))
                .executeUpdate();
        return lastInsertId();
    }

    /** 실제 POST /api/posts를 통해 만들어서(직접 insert보다 실제 흐름을 그대로 탄다) place·image까지 채운다. */
    private Long createPost(Long ownerUserId, boolean publish) throws Exception {
        Long tripId = createTripFixture(ownerUserId);
        Long tripDayId = createTripDayFixture(tripId);
        Long placeId = createPlaceFixture();
        createTripPlaceFixture(tripId, tripDayId, placeId);
        Long fileId = createFileFixture(ownerUserId);

        authenticateAs(ownerUserId);
        String body = "{\"tripId\":" + tripId + ",\"title\":\"제목\",\"content\":\"본문\","
                + "\"imageFileIds\":[" + fileId + "],\"placeIds\":[" + placeId + "],"
                + "\"visibility\":\"PUBLIC\",\"publish\":" + publish + "}";
        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        SecurityContextHolder.clearContext();
        return Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));
    }

    @Test
    void 작성자가_삭제하면_204이고_상세는_404가_된다() throws Exception {
        Long postId = createPost(USER_ID, true);

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/{postId}", postId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 삭제된_게시글은_내_목록에서도_제외된다() throws Exception {
        Long postId = createPost(USER_ID, true);
        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 삭제하면_포토맵_방문_지역_수가_줄어든다() throws Exception {
        Long postId = createPost(USER_ID, true);
        assertThat(photoMapService.getRegions(USER_ID).totalVisitedRegionCount()).isEqualTo(1);

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
        entityManager.clear();

        assertThat(photoMapService.getRegions(USER_ID).totalVisitedRegionCount()).isEqualTo(0);
    }

    @Test
    void 삭제하면_Trip_경로_잠금이_해제되고_같은_Trip에_새_게시글을_만들_수_있다() throws Exception {
        Long postId = createPost(USER_ID, true);

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
        entityManager.clear();

        Long tripId = ((Number) entityManager.createNativeQuery(
                        "select trip_id from posts where post_id = :id")
                .setParameter("id", postId).getSingleResult()).longValue();

        String body = "{\"tripId\":" + tripId + ",\"title\":\"재작성\",\"content\":\"본문\","
                + "\"visibility\":\"PUBLIC\",\"publish\":true}";
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void 타인이_저장한_경로는_유지되지만_접근불가로_바뀐다() throws Exception {
        Long postId = createPost(USER_ID, true);
        SavedRoute savedRoute = savedRouteRepository.save(
                SavedRoute.of(OTHER_USER_ID, SavedRouteSourceType.POST, postId));

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
        entityManager.clear();

        SavedRoute reloaded = savedRouteRepository.findById(savedRoute.getSavedRouteId()).orElseThrow();
        assertThat(reloaded.isSourceAvailable()).isFalse();
    }

    @Test
    void 삭제하면_연결된_이미지_파일_행도_삭제된다() throws Exception {
        Long postId = createPost(USER_ID, true);
        Long fileId = postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(postId)
                .get(0).getId().getFileId();

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
        // fileAssetRepository.deleteAll()의 pending remove가 flush 전에 clear되면 조용히
        // 사라진다(entityManager.clear()는 flush하지 않음) — 명시적으로 flush 먼저.
        entityManager.flush();
        entityManager.clear();

        Long remaining = ((Number) entityManager.createNativeQuery(
                        "select count(*) from files where file_id = :id")
                .setParameter("id", fileId).getSingleResult()).longValue();
        assertThat(remaining).isZero();
    }

    @Test
    void 비작성자가_삭제하면_403이다() throws Exception {
        Long postId = createPost(USER_ID, true);

        authenticateAs(OTHER_USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_ACCESS_DENIED"));
    }

    @Test
    void 이미_삭제된_게시글을_작성자가_다시_삭제하면_멱등하게_204다() throws Exception {
        Long postId = createPost(USER_ID, true);
        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
    }

    @Test
    void 존재하지_않는_게시글_삭제는_404다() throws Exception {
        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void DRAFT_게시글도_작성자가_삭제할_수_있다() throws Exception {
        Long postId = createPost(USER_ID, false);

        authenticateAs(USER_ID);
        mockMvc.perform(delete("/api/posts/{postId}", postId)).andExpect(status().isNoContent());
    }
}
