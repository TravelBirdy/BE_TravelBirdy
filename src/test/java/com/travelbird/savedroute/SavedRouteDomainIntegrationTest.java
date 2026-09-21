package com.travelbird.savedroute;

import com.travelbird.ai.api.AiPreviewSavedRouteService;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.savedroute.api.SavedRouteReferenceReader;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 경로 저장·취소·목록 조회 통합 테스트. backend-functional-spec-v10.md §3.10.
 * {@code CommunityDomainIntegrationTest}와 동일한 격리된 로컬 MySQL+native SQL fixture 패턴.
 *
 * <p>AI_PREVIEW 흐름은 {@link AiPreviewSavedRouteService}(Part2)를 {@code @MockitoBean}으로
 * 대체한다 — 실제 AI 미리보기 fixture(ai_recommendation_jobs/ai_trip_previews 등)를 세팅하지
 * 않고 Part3 쪽 위임·멱등 로직만 검증한다(파트 간 호출 경계 — Part2 내부 로직은 Part2 책임).
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class SavedRouteDomainIntegrationTest {


    private static final String SIGUNGU_CODE = "11110";
    private static final Long SAVER_USER_ID = 1L;
    private static final Long AUTHOR_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private SavedRouteRepository savedRouteRepository;
    @Autowired
    private SavedRouteReferenceReader savedRouteReferenceReader;
    @MockitoBean
    private AiPreviewSavedRouteService aiPreviewSavedRouteService;

    @BeforeEach
    void seedFixtures() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", SAVER_USER_ID)
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", AUTHOR_USER_ID)
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

    private Post createPublishedPost(Long ownerUserId, PostVisibility visibility) {
        Long tripId = createTripFixture(ownerUserId);
        return postRepository.saveAndFlush(Post.create(tripId, "제목", "본문", null, visibility, true));
    }

    private void markBlocked(Long postId) {
        entityManager.createNativeQuery("update posts set status = 'BLOCKED' where post_id = :id")
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
    }

    private void block(Long blockerUserId, Long blockedUserId) {
        entityManager.createNativeQuery(
                        "insert into user_blocks (blocker_user_id, blocked_user_id) values (:blocker, :blocked)")
                .setParameter("blocker", blockerUserId)
                .setParameter("blocked", blockedUserId)
                .executeUpdate();
    }

    // ===== 게시글 경로 저장·취소 =====

    @Test
    void 게시글_경로를_저장하면_saveCount가_증가한다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        // incrementSaveCount는 벌크 업데이트라 1차 캐시에 남은 post(생성 시 로드)를 갱신하지 않는다.
        entityManager.clear();
        Post reloaded = postRepository.findById(post.getPostId()).orElseThrow();
        assertThat(reloaded.getSaveCount()).isEqualTo(1L);
        assertThat(savedRouteRepository.existsByUserIdAndSourceTypeAndSourceId(
                SAVER_USER_ID, SavedRouteSourceType.POST, post.getPostId())).isTrue();
    }

    @Test
    void 자신의_게시글은_저장할_수_없다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(AUTHOR_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_SAVE_OWN_ROUTE"));
    }

    @Test
    void 존재하지_않는_게시글_저장은_404다() throws Exception {
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 비공개_게시글_저장은_404다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PRIVATE);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 차단_관계인_작성자의_게시글_저장은_404다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        block(SAVER_USER_ID, AUTHOR_USER_ID);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 중복_저장은_멱등하고_saveCount는_한번만_증가한다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());
        entityManager.clear();
        assertThat(postRepository.findById(post.getPostId()).orElseThrow().getSaveCount()).isEqualTo(1L);
    }

    @Test
    void 저장_취소하면_saveCount가_감소하고_목록에서_사라진다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        // deleteByUserIdAndSourceTypeAndSourceId는 엔티티를 찾아 remove()로 지우는 파생 쿼리라
        // 커밋 전까지 flush가 지연될 수 있다 — clear()가 그 pending remove를 날려버리기 전에 먼저 flush한다.
        entityManager.flush();
        entityManager.clear();
        assertThat(postRepository.findById(post.getPostId()).orElseThrow().getSaveCount()).isEqualTo(0L);
        assertThat(savedRouteRepository.existsByUserIdAndSourceTypeAndSourceId(
                SAVER_USER_ID, SavedRouteSourceType.POST, post.getPostId())).isFalse();
    }

    @Test
    void 저장하지_않은_경로_취소는_멱등하게_204다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(delete("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        entityManager.clear();
        assertThat(postRepository.findById(post.getPostId()).orElseThrow().getSaveCount()).isEqualTo(0L);
    }

    @Test
    void 존재하지_않는_게시글_경로_취소는_404다() throws Exception {
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(delete("/api/users/me/saved-routes/posts/{postId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 삭제된_게시글_경로_취소도_404다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());
        entityManager.createNativeQuery("update posts set deleted_at = now() where post_id = :id")
                .setParameter("id", post.getPostId())
                .executeUpdate();
        entityManager.clear();

        mockMvc.perform(delete("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 비로그인_저장_요청은_401이다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);

        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isUnauthorized());
    }

    // ===== 목록 조회 =====

    @Test
    void 목록_조회는_저장일_내림차순이고_커서로_다음_페이지를_가져온다() throws Exception {
        Post post1 = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        Post post2 = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post1.getPostId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post2.getPostId()))
                .andExpect(status().isNoContent());

        String first = mockMvc.perform(get("/api/users/me/saved-routes").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].sourceId").value(post2.getPostId()))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn().getResponse().getContentAsString();

        Long nextCursor = Long.valueOf(first.split("\"nextCursor\":")[1].replaceAll("[^0-9].*", ""));

        mockMvc.perform(get("/api/users/me/saved-routes").param("size", "1").param("cursor", nextCursor.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].sourceId").value(post1.getPostId()))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void 게시글_경로_항목은_실제_카드_데이터를_포함한다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/saved-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("제목"))
                .andExpect(jsonPath("$.items[0].region.sigunguCode").value(SIGUNGU_CODE))
                .andExpect(jsonPath("$.items[0].author.userId").value(AUTHOR_USER_ID))
                .andExpect(jsonPath("$.items[0].editable").value(false))
                .andExpect(jsonPath("$.items[0].sourceAvailable").value(true));
    }

    @Test
    void 저장_이후_게시글이_차단되면_목록에서_빠지고_DB에도_반영된다() throws Exception {
        Post post = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", post.getPostId()))
                .andExpect(status().isNoContent());

        markBlocked(post.getPostId());

        mockMvc.perform(get("/api/users/me/saved-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        // self-heal의 markUnavailable도 벌크 업데이트라 1차 캐시(저장 시 로드된 SavedRoute)를 갱신하지 않는다.
        entityManager.clear();
        assertThat(savedRouteRepository.findByUserIdAndSourceTypeAndSourceId(
                        SAVER_USER_ID, SavedRouteSourceType.POST, post.getPostId())
                .orElseThrow().isSourceAvailable()).isFalse();
    }

    @Test
    void 최근_저장한_항목이_접근불가여도_size만큼_다음_항목으로_채워진다() throws Exception {
        Post older = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        Post newer = createPublishedPost(AUTHOR_USER_ID, PostVisibility.PUBLIC);
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", older.getPostId()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/users/me/saved-routes/posts/{postId}", newer.getPostId()))
                .andExpect(status().isNoContent());

        markBlocked(newer.getPostId()); // 가장 최근 저장(맨 앞에 와야 할) 항목을 접근 불가로 만든다.

        // size=1로 요청해도, 맨 앞 항목이 self-heal로 빠지면 그 다음 항목까지 이어서 채워야
        // 한다 — 한 번만 조회하고 끝내면 items=[]에 nextCursor만 남아 프론트가 끝으로
        // 오해할 수 있다(oriole0419 PR#14 리뷰).
        mockMvc.perform(get("/api/users/me/saved-routes").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].sourceId").value(older.getPostId()))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void 목록_조회_비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me/saved-routes"))
                .andExpect(status().isUnauthorized());
    }

    // ===== AI 미리보기 경로 저장·취소 =====

    @Test
    void AI_미리보기_저장은_Part2_저장을_위임하고_SavedRoute를_생성한다() throws Exception {
        Long previewId = 100L;
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isNoContent());

        verify(aiPreviewSavedRouteService).save(eq(SAVER_USER_ID), eq(previewId));
        assertThat(savedRouteRepository.existsByUserIdAndSourceTypeAndSourceId(
                SAVER_USER_ID, SavedRouteSourceType.AI_PREVIEW, previewId)).isTrue();
        assertThat(savedRouteReferenceReader.existsAvailableAiPreviewReference(previewId)).isTrue();
    }

    @Test
    void AI_미리보기_저장이_Part2에서_실패하면_SavedRoute를_만들지_않는다() throws Exception {
        Long previewId = 200L;
        Mockito.doThrow(new com.travelbird.global.error.BusinessException(
                        com.travelbird.global.error.ErrorCode.AI_PREVIEW_EXPIRED))
                .when(aiPreviewSavedRouteService).save(SAVER_USER_ID, previewId);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(put("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("AI_PREVIEW_EXPIRED"));

        assertThat(savedRouteRepository.existsByUserIdAndSourceTypeAndSourceId(
                SAVER_USER_ID, SavedRouteSourceType.AI_PREVIEW, previewId)).isFalse();
    }

    @Test
    void AI_미리보기_취소는_마지막_참조_삭제_후_Part2_강등을_호출한다() throws Exception {
        Long previewId = 300L;
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isNoContent());

        verify(aiPreviewSavedRouteService).cancel(eq(SAVER_USER_ID), eq(previewId));
        assertThat(savedRouteReferenceReader.existsAvailableAiPreviewReference(previewId)).isFalse();
    }

    @Test
    void 저장하지_않았지만_유효한_AI_미리보기_취소는_Part2_검증을_거쳐_멱등하게_204다() throws Exception {
        Long previewId = 400L;
        authenticateAs(SAVER_USER_ID);

        // 로컬 SavedRoute 행이 없어도(=이미 취소됐거나 애초에 저장한 적 없음) Part2 cancel()은
        // 항상 호출한다 — makeTemporary()가 이미 TEMPORARY면 멱등이라 여기선 예외 없이 204다.
        mockMvc.perform(delete("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isNoContent());

        verify(aiPreviewSavedRouteService).cancel(eq(SAVER_USER_ID), eq(previewId));
    }

    @Test
    void 존재하지_않거나_남의_AI_미리보기_취소는_Part2_예외가_그대로_전파된다() throws Exception {
        Long previewId = 401L;
        Mockito.doThrow(new com.travelbird.global.error.BusinessException(
                        com.travelbird.global.error.ErrorCode.AI_PREVIEW_ACCESS_DENIED))
                .when(aiPreviewSavedRouteService).cancel(SAVER_USER_ID, previewId);
        authenticateAs(SAVER_USER_ID);

        mockMvc.perform(delete("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AI_PREVIEW_ACCESS_DENIED"));
    }

    @Test
    void AI_미리보기_목록_항목은_editable이_true다() throws Exception {
        Long previewId = 500L;
        authenticateAs(SAVER_USER_ID);
        mockMvc.perform(put("/api/users/me/saved-routes/ai-previews/{previewId}", previewId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/saved-routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sourceType").value("AI_PREVIEW"))
                .andExpect(jsonPath("$.items[0].editable").value(true));
    }
}
