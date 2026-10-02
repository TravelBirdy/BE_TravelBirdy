package com.travelbird.post;

import com.travelbird.file.client.S3FileStorage;
import com.travelbird.post.repository.PostHashtagRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 수정(`PATCH /api/posts/{postId}`) 통합 테스트. backend-functional-spec-v10.md
 * §3.8.4. 픽스처 패턴은 {@link PostCreateIntegrationTest}와 동일한 Local MySQL+MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PostUpdateIntegrationTest {

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
    @Autowired
    private PostHashtagRepository postHashtagRepository;
    @MockitoBean
    private S3FileStorage s3FileStorage;

    @BeforeEach
    void seedFixtures() {
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

    private Long createFileFixture(Long ownerUserId, String status) {
        entityManager.createNativeQuery(
                        "insert into files (user_id, file_name, object_key, content_type, size_bytes, width, "
                                + "height, purpose, status, expires_at) values (:userId, 'a.jpg', :objectKey, "
                                + "'image/jpeg', 1024, 100, 100, 'POST', :status, :expiresAt)")
                .setParameter("userId", ownerUserId)
                .setParameter("objectKey", "post/" + UUID.randomUUID())
                .setParameter("status", status)
                .setParameter("expiresAt", LocalDateTime.now().plusDays(1))
                .executeUpdate();
        Number fileId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();
        return fileId.longValue();
    }

    private record CreatedPost(Long postId, Long version) {
    }

    private CreatedPost createPost(Long ownerUserId, Long tripId, boolean publish) throws Exception {
        authenticateAs(ownerUserId);
        String body = "{\"tripId\":" + tripId + ",\"title\":\"원래 제목\",\"content\":\"원래 본문\","
                + "\"visibility\":\"PUBLIC\",\"publish\":" + publish + "}";
        String response = mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long postId = Long.valueOf(response.split("\"postId\":")[1].replaceAll("[^0-9].*", ""));
        Long version = Long.valueOf(response.split("\"version\":")[1].replaceAll("[^0-9].*", ""));
        return new CreatedPost(postId, version);
    }

    @Test
    void DRAFT_제목_본문_공개범위를_수정할_수_있다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"title\":\"수정된 제목\",\"content\":\"수정된 본문\",\"visibility\":\"PRIVATE\","
                + "\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.content").value("수정된 본문"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.version").value(created.version() + 1));
    }

    @Test
    void publish_true면_DRAFT가_발행된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"publish\":true,\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertThat(postRepository.findById(created.postId()).orElseThrow().isRouteLocked()).isTrue();
    }

    @Test
    void 발행_상태에서_제목을_explicit_null로_비우면_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, true);

        String body = "{\"title\":null,\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POST_CONTENT"));
    }

    @Test
    void 발행된_게시글의_placeIds_변경은_409다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithPlace(tripId);
        CreatedPost created = createPost(USER_ID, tripId, true);

        String body = "{\"placeIds\":[" + placeId + "],\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_ROUTE_LOCKED_AFTER_PUBLISH"));
    }

    @Test
    void DRAFT에서_발행과_동시에_placeIds를_확정할_수_있다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long placeId = createTripWithPlace(tripId);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"placeIds\":[" + placeId + "],\"publish\":true,\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertThat(postPlaceRepository.findByIdPostId(created.postId())).hasSize(1);
    }

    @Test
    void 버전이_다르면_409다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"title\":\"수정\",\"version\":" + (created.version() + 1) + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POST_MODIFICATION_CONFLICT"));
    }

    @Test
    void version_필드를_보내지_않으면_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void visibility에_explicit_null은_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"visibility\":null,\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void representativeFileId가_imageFileIds_밖이면_400이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long fileId = createFileFixture(USER_ID, "UPLOADED");
        Long otherFileId = createFileFixture(USER_ID, "UPLOADED");
        CreatedPost created = createPost(USER_ID, tripId, false);

        String body = "{\"imageFileIds\":[" + fileId + "],\"representativeFileId\":" + otherFileId
                + ",\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void imageFileIds_교체시_추가분은_링크되고_제거분은_S3에서_삭제된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long oldFileId = createFileFixture(USER_ID, "UPLOADED");
        CreatedPost created = createPost(USER_ID, tripId, false);

        String attachBody = "{\"imageFileIds\":[" + oldFileId + "],\"version\":" + created.version() + "}";
        String response = mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(attachBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long versionAfterAttach = Long.valueOf(response.split("\"version\":")[1].replaceAll("[^0-9].*", ""));

        Long newFileId = createFileFixture(USER_ID, "UPLOADED");
        String replaceBody = "{\"imageFileIds\":[" + newFileId + "],\"version\":" + versionAfterAttach + "}";
        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(replaceBody))
                .andExpect(status().isOk());

        List<Long> remaining = postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(created.postId()).stream()
                .map(image -> image.getId().getFileId()).toList();
        assertThat(remaining).containsExactly(newFileId);
        verify(s3FileStorage).delete(org.mockito.ArgumentMatchers.argThat(key -> key != null));
    }

    @Test
    void imageFileIds_교체로_대표이미지가_밀려나면_자동으로_연결이_해제된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        Long representativeFileId = createFileFixture(USER_ID, "UPLOADED");
        CreatedPost created = createPost(USER_ID, tripId, false);

        String attachBody = "{\"imageFileIds\":[" + representativeFileId + "],\"representativeFileId\":"
                + representativeFileId + ",\"version\":" + created.version() + "}";
        String response = mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(attachBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long versionAfterAttach = Long.valueOf(response.split("\"version\":")[1].replaceAll("[^0-9].*", ""));

        Long otherFileId = createFileFixture(USER_ID, "UPLOADED");
        String replaceBody = "{\"imageFileIds\":[" + otherFileId + "],\"version\":" + versionAfterAttach + "}";
        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(replaceBody))
                .andExpect(status().isOk());

        assertThat(postRepository.findById(created.postId()).orElseThrow().getRepresentativeFileId()).isNull();
    }

    @Test
    void hashtags를_빈배열로_보내면_전체_삭제된다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);

        String addBody = "{\"hashtags\":[\"바다\",\"여름\"],\"version\":" + created.version() + "}";
        String response = mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(addBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long versionAfterAdd = Long.valueOf(response.split("\"version\":")[1].replaceAll("[^0-9].*", ""));
        assertThat(postHashtagRepository.findByIdPostId(created.postId())).hasSize(2);

        String clearBody = "{\"hashtags\":[],\"version\":" + versionAfterAdd + "}";
        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(clearBody))
                .andExpect(status().isOk());

        assertThat(postHashtagRepository.findByIdPostId(created.postId())).isEmpty();
    }

    @Test
    void 비작성자_수정은_403이다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);
        authenticateAs(OTHER_USER_ID);

        String body = "{\"title\":\"수정\",\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("POST_ACCESS_DENIED"));
    }

    @Test
    void 존재하지_않는_게시글은_404다() throws Exception {
        authenticateAs(USER_ID);

        mockMvc.perform(patch("/api/posts/{postId}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"수정\",\"version\":0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 이미_삭제된_게시글_수정은_204다() throws Exception {
        Long tripId = createTripFixture(USER_ID);
        CreatedPost created = createPost(USER_ID, tripId, false);
        entityManager.createNativeQuery("update posts set deleted_at = now() where post_id = :id")
                .setParameter("id", created.postId())
                .executeUpdate();
        entityManager.clear();

        String body = "{\"title\":\"수정\",\"version\":" + created.version() + "}";

        mockMvc.perform(patch("/api/posts/{postId}", created.postId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());
    }
}
