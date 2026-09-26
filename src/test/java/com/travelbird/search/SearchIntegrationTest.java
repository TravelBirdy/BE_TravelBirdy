package com.travelbird.search;

import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostVisibility;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합검색 {@code GET /api/search} 통합 테스트. backend-functional-spec-v10.md §3.16.1.
 * 인증은 {@code SecurityContextHolder}로 직접 주입하고(addFilters=false), 실제 보안 필터를
 * 거친 401은 {@link SearchSecurityFilterIntegrationTest}에서 따로 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class SearchIntegrationTest {

    private static final String JONGNO = "11110";
    private static final String JUNG = "11140";
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void seedUsersAndAuth() {
        insertUser(USER_ID);
        insertUser(OTHER_USER_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
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

    private Long lastInsertId() {
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
    }

    private Long createPlace(String name, String address, String sigunguCode, String status) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values (:status, :name, 'ATTRACTION', :address, :sigunguCode, 37.5, 127.0)")
                .setParameter("status", status)
                .setParameter("name", name)
                .setParameter("address", address)
                .setParameter("sigunguCode", sigunguCode)
                .executeUpdate();
        return lastInsertId();
    }

    private Long createPlace(String name, String address, String sigunguCode) {
        return createPlace(name, address, sigunguCode, "ACTIVE");
    }

    private Long createTrip(Long ownerUserId) {
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", ownerUserId)
                .setParameter("sigunguCode", JONGNO)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        return lastInsertId();
    }

    private Post createPost(Long ownerUserId, String title, PostVisibility visibility, boolean publish) {
        return postRepository.saveAndFlush(
                Post.create(createTrip(ownerUserId), title, "본문", null, visibility, publish));
    }

    private void markBlocked(Long postId) {
        entityManager.createNativeQuery("update posts set status = 'BLOCKED' where post_id = :id")
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
    }

    // ===== 검증 =====

    @Test
    void 빈_검색어는_400이다() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_REQUIRED"));
        mockMvc.perform(get("/api/search").param("query", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_REQUIRED"));
    }

    @Test
    void 검색어_51자는_400이다() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "가".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_TOO_LONG"));
    }

    // ===== 장소 =====

    @Test
    void 장소는_이름과_주소로_검색하고_이름_일치가_먼저_나온다() throws Exception {
        createPlace("종로 카페", "서울 종로구 어딘가 1", JONGNO);
        createPlace("경복궁", "서울 종로구 사직로 161 카페거리", JONGNO);
        createPlace("남산타워", "서울 용산구 남산공원길", "11170");

        mockMvc.perform(get("/api/search").param("query", "카페"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.places[0].name").value("종로 카페"))
                .andExpect(jsonPath("$.places[1].name").value("경복궁"))
                .andExpect(jsonPath("$.places[0].externalPlaceId").doesNotExist())
                .andExpect(jsonPath("$.hasMorePlaces").value(false));
    }

    @Test
    void 폐업_장소는_검색되지_않는다() throws Exception {
        createPlace("문닫은 카페", "서울 종로구", JONGNO, "CLOSED");

        mockMvc.perform(get("/api/search").param("query", "카페"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0));
    }

    @Test
    void 장소는_기본_5개까지만_반환하고_hasMorePlaces가_true다() throws Exception {
        for (int i = 1; i <= 6; i++) {
            createPlace("맛집" + i, "서울 종로구", JONGNO);
        }

        mockMvc.perform(get("/api/search").param("query", "맛집"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(5))
                .andExpect(jsonPath("$.hasMorePlaces").value(true));
    }

    @Test
    void limitPerType은_최대_5로_제한되고_작게_줄일_수_있다() throws Exception {
        for (int i = 1; i <= 6; i++) {
            createPlace("맛집" + i, "서울 종로구", JONGNO);
        }

        mockMvc.perform(get("/api/search").param("query", "맛집").param("limitPerType", "2"))
                .andExpect(jsonPath("$.places.length()").value(2))
                .andExpect(jsonPath("$.hasMorePlaces").value(true));
        mockMvc.perform(get("/api/search").param("query", "맛집").param("limitPerType", "99"))
                .andExpect(jsonPath("$.places.length()").value(5));
    }

    @Test
    void 검색어의_LIKE_특수문자는_문자_그대로_검색된다() throws Exception {
        createPlace("100%맛집", "서울 종로구", JONGNO);
        createPlace("일반식당", "서울 종로구", JONGNO);

        mockMvc.perform(get("/api/search").param("query", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].name").value("100%맛집"));
    }

    @Test
    void 저장한_장소는_saved가_true다() throws Exception {
        Long saved = createPlace("저장한 카페", "서울 종로구", JONGNO);
        createPlace("안저장 카페", "서울 종로구", JONGNO);
        entityManager.createNativeQuery("insert into saved_places (user_id, place_id) values (:u, :p)")
                .setParameter("u", USER_ID)
                .setParameter("p", saved)
                .executeUpdate();

        mockMvc.perform(get("/api/search").param("query", "카페"))
                .andExpect(jsonPath("$.places[?(@.name=='저장한 카페')].saved").value(true))
                .andExpect(jsonPath("$.places[?(@.name=='안저장 카페')].saved").value(false));
    }

    // ===== 지역 =====

    @Test
    void 지역은_시군구명으로_검색한다() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "종로"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regions.length()").value(1))
                .andExpect(jsonPath("$.regions[0].sigunguCode").value(JONGNO))
                .andExpect(jsonPath("$.regions[0].sigunguName").value("종로구"));
    }

    @Test
    void 지역은_전국_시군구가_대상이다() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "강서구"))
                .andExpect(jsonPath("$.regions.length()").value(2)) // 서울 강서구·부산 강서구
                .andExpect(jsonPath("$.hasMoreRegions").value(false));
    }

    // ===== 기록 =====

    @Test
    void 기록은_공개_발행_게시글만_검색된다() throws Exception {
        createPost(USER_ID, "제주 여행기", PostVisibility.PUBLIC, true);
        createPost(USER_ID, "제주 메모비공개", PostVisibility.MEMO_PRIVATE, true);
        createPost(USER_ID, "제주 비공개", PostVisibility.PRIVATE, true);
        createPost(USER_ID, "제주 임시저장", PostVisibility.PUBLIC, false);
        Post blocked = createPost(USER_ID, "제주 차단글", PostVisibility.PUBLIC, true);
        markBlocked(blocked.getPostId());

        mockMvc.perform(get("/api/search").param("query", "제주"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posts.length()").value(2));
    }

    @Test
    void 차단한_사용자의_게시글은_검색되지_않는다() throws Exception {
        createPost(OTHER_USER_ID, "제주 여행기", PostVisibility.PUBLIC, true);
        entityManager.createNativeQuery(
                        "insert into user_blocks (blocker_user_id, blocked_user_id) values (:blocker, :blocked)")
                .setParameter("blocker", USER_ID)
                .setParameter("blocked", OTHER_USER_ID)
                .executeUpdate();

        mockMvc.perform(get("/api/search").param("query", "제주"))
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void 기록도_limitPerType만큼만_반환하고_hasMorePosts가_true다() throws Exception {
        for (int i = 1; i <= 3; i++) {
            createPost(USER_ID, "제주 여행기" + i, PostVisibility.PUBLIC, true);
        }

        mockMvc.perform(get("/api/search").param("query", "제주").param("limitPerType", "2"))
                .andExpect(jsonPath("$.posts.length()").value(2))
                .andExpect(jsonPath("$.hasMorePosts").value(true));
    }

    // ===== sigunguCodes =====

    @Test
    void sigunguCodes는_장소와_지역에만_적용된다() throws Exception {
        createPlace("종로 카페", "서울 종로구", JONGNO);
        createPlace("중구 카페", "서울 중구", JUNG);
        createPost(USER_ID, "카페 투어", PostVisibility.PUBLIC, true);

        mockMvc.perform(get("/api/search").param("query", "카페").param("sigunguCodes", JONGNO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(1))
                .andExpect(jsonPath("$.places[0].name").value("종로 카페"))
                .andExpect(jsonPath("$.posts.length()").value(1));

        mockMvc.perform(get("/api/search").param("query", "구").param("sigunguCodes", JUNG))
                .andExpect(jsonPath("$.regions.length()").value(1))
                .andExpect(jsonPath("$.regions[0].sigunguCode").value(JUNG));
    }

    @Test
    void 빈_sigunguCodes나_유효하지_않은_코드만_주면_전부_빈_결과다() throws Exception {
        createPlace("종로 카페", "서울 종로구", JONGNO);
        createPost(USER_ID, "카페 투어", PostVisibility.PUBLIC, true);

        mockMvc.perform(get("/api/search").param("query", "카페").param("sigunguCodes", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.posts.length()").value(0))
                .andExpect(jsonPath("$.regions.length()").value(0));
        mockMvc.perform(get("/api/search").param("query", "카페").param("sigunguCodes", "99999"))
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.posts.length()").value(0));
    }

    @Test
    void 정상_응답은_unavailableSections가_비어있다() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "종로"))
                .andExpect(jsonPath("$.unavailableSections.length()").value(0));
    }
}
