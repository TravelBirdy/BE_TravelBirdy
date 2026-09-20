package com.travelbird.community;

import com.travelbird.community.service.CommunityPostSearchService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 커뮤니티 전체/인기 목록·검색·공유수 증가·신고 통합 테스트.
 * backend-functional-spec-v10.md §3.9.1~§3.9.3, §3.9.5, §3.9.6.
 * {@code PostDomainIntegrationTest}와 동일한 Testcontainers+native SQL fixture 패턴.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CommunityDomainIntegrationTest {

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
    private static final Long AUTHOR_USER_ID = 3L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommunityPostSearchService communityPostSearchService;

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

    private Long createTripFixture() {
        return createTripFixture(USER_ID);
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

    private Post createPublishedPost(String title, String content, PostVisibility visibility) {
        return createPublishedPost(USER_ID, title, content, visibility);
    }

    private Post createPublishedPost(Long ownerUserId, String title, String content, PostVisibility visibility) {
        Long tripId = createTripFixture(ownerUserId);
        return postRepository.saveAndFlush(Post.create(tripId, title, content, null, visibility, true));
    }

    private void markBlocked(Long postId) {
        // 운영자가 DB에서 직접 status=BLOCKED로 바꾸는 실제 운영 방식(§3.9.6)을 그대로 흉내낸다 —
        // 애플리케이션 코드엔 BLOCKED로의 전이 메서드가 없다(관리자 API 자체가 없어서).
        entityManager.createNativeQuery("update posts set status = 'BLOCKED' where post_id = :id")
                .setParameter("id", postId)
                .executeUpdate();
        entityManager.clear();
    }

    private void follow(Long followerUserId, Long followingUserId) {
        entityManager.createNativeQuery(
                        "insert into follows (follower_user_id, following_user_id) values (:follower, :following)")
                .setParameter("follower", followerUserId)
                .setParameter("following", followingUserId)
                .executeUpdate();
    }

    private void block(Long blockerUserId, Long blockedUserId) {
        entityManager.createNativeQuery(
                        "insert into user_blocks (blocker_user_id, blocked_user_id) values (:blocker, :blocked)")
                .setParameter("blocker", blockerUserId)
                .setParameter("blocked", blockedUserId)
                .executeUpdate();
    }

    private void insertViewHistory(Long postId, LocalDateTime viewedAt) {
        entityManager.createNativeQuery(
                        "insert into post_view_histories (post_id, viewer_user_id, viewed_at) values (:postId, :userId, :viewedAt)")
                .setParameter("postId", postId)
                .setParameter("userId", OTHER_USER_ID)
                .setParameter("viewedAt", viewedAt)
                .executeUpdate();
    }

    private void insertShareHistory(Long postId, LocalDateTime createdAt) {
        entityManager.createNativeQuery(
                        "insert into post_shares (post_id, user_id, channel, created_at) values (:postId, :userId, 'LINK', :createdAt)")
                .setParameter("postId", postId)
                .setParameter("userId", OTHER_USER_ID)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }

    // ===== ALL 탭 =====

    @Test
    void ALL_탭은_공개_가능한_게시글만_반환한다() throws Exception {
        createPublishedPost("공개글", "본문", PostVisibility.PUBLIC);
        createPublishedPost("비공개글", "본문", PostVisibility.PRIVATE);
        Post blocked = createPublishedPost("차단글", "본문", PostVisibility.PUBLIC);
        markBlocked(blocked.getPostId());

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("공개글"));
    }

    @Test
    void ALL_탭은_비로그인도_조회_가능하다() throws Exception {
        createPublishedPost("공개글", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL"))
                .andExpect(status().isOk());
    }

    @Test
    void ALL_탭_커서로_다음_페이지를_가져온다() throws Exception {
        createPublishedPost("1번", "본문", PostVisibility.PUBLIC);
        createPublishedPost("2번", "본문", PostVisibility.PUBLIC);
        createPublishedPost("3번", "본문", PostVisibility.PUBLIC);

        String first = mockMvc.perform(get("/api/community/posts").param("tab", "ALL").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.nextCursor").exists())
                .andReturn().getResponse().getContentAsString();

        Long nextCursor = Long.valueOf(first.split("\"nextCursor\":")[1].replaceAll("[^0-9].*", ""));

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL").param("size", "2")
                        .param("cursor", nextCursor.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void ALL_탭_존재하지_않는_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/community/posts").param("tab", "ALL").param("cursor", "999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    void ALL_탭_삭제된_게시글_커서로도_정상_재개된다() throws Exception {
        // 커서는 "다음 페이지"의 시작점이라 남은글보다 최신(더 큰 postId)이어야 의미가 있다 —
        // DESC 정렬에서 커서보다 오래된 글이 다음 페이지에 나온다.
        createPublishedPost("남은글", "본문", PostVisibility.PUBLIC);
        Post cursorPost = createPublishedPost("삭제될글", "본문", PostVisibility.PUBLIC);
        cursorPost.tombstone();
        postRepository.saveAndFlush(cursorPost);

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL")
                        .param("cursor", cursorPost.getPostId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("남은글"));
    }

    @Test
    void 정의되지_않은_탭은_400이다() throws Exception {
        mockMvc.perform(get("/api/community/posts").param("tab", "TRENDING"))
                .andExpect(status().isBadRequest());
    }

    // ===== POPULAR 탭 =====

    @Test
    void POPULAR_탭은_기간내_점수순으로_반환한다() throws Exception {
        Post lowScore = createPublishedPost("조회1회", "본문", PostVisibility.PUBLIC);
        Post highScore = createPublishedPost("공유1회", "본문", PostVisibility.PUBLIC);
        insertViewHistory(lowScore.getPostId(), LocalDateTime.now());
        insertShareHistory(highScore.getPostId(), LocalDateTime.now());

        mockMvc.perform(get("/api/community/posts").param("tab", "POPULAR").param("period", "MONTH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("공유1회"))
                .andExpect(jsonPath("$.items[1].title").value("조회1회"));
    }

    @Test
    void POPULAR_탭_기간_밖_이벤트는_점수에_포함되지_않는다() throws Exception {
        Post oldEventPost = createPublishedPost("오래된조회", "본문", PostVisibility.PUBLIC);
        Post recentEventPost = createPublishedPost("최근조회", "본문", PostVisibility.PUBLIC);
        insertViewHistory(oldEventPost.getPostId(), LocalDateTime.now().minusDays(8));
        insertViewHistory(recentEventPost.getPostId(), LocalDateTime.now());

        mockMvc.perform(get("/api/community/posts").param("tab", "POPULAR").param("period", "WEEK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("최근조회"));
    }

    @Test
    void POPULAR_탭_존재하지_않는_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/community/posts").param("tab", "POPULAR").param("cursor", "999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    // ===== 검색 =====

    @Test
    void 검색어가_50자_초과면_400이다() throws Exception {
        String longQuery = "가".repeat(51);
        mockMvc.perform(get("/api/community/posts/search").param("query", longQuery))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_TOO_LONG"));
    }

    @Test
    void 태그_정확일치가_제목_포함보다_우선한다() throws Exception {
        Post tagMatch = createPublishedPost("여행 이야기", "본문", PostVisibility.PUBLIC);
        entityManager.createNativeQuery("insert into post_hashtags (post_id, hashtag) values (:postId, '부산')")
                .setParameter("postId", tagMatch.getPostId())
                .executeUpdate();
        createPublishedPost("부산 여행 후기", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(get("/api/community/posts/search").param("query", "부산"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("여행 이야기"))
                .andExpect(jsonPath("$.items[1].title").value("부산 여행 후기"));
    }

    @Test
    void 제목_포함이_본문_포함보다_우선한다() throws Exception {
        createPublishedPost("여행 이야기", "제주 갔다왔어요", PostVisibility.PUBLIC);
        createPublishedPost("제주 여행", "재밌었다", PostVisibility.PUBLIC);

        mockMvc.perform(get("/api/community/posts/search").param("query", "제주"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("제주 여행"))
                .andExpect(jsonPath("$.items[1].title").value("여행 이야기"));
    }

    @Test
    void LIKE_특수문자는_이스케이프되어_와일드카드로_동작하지_않는다() throws Exception {
        createPublishedPost("할인 50% 특가", "본문", PostVisibility.PUBLIC);
        // 이스케이프가 깨지면 "50%"가 "50 뒤에 아무거나"로 해석돼 이 글도 잘못 매치된다.
        createPublishedPost("50가지 무관한 이야기", "아무 내용", PostVisibility.PUBLIC);

        mockMvc.perform(get("/api/community/posts/search").param("query", "50%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("할인 50% 특가"));
    }

    @Test
    void 검색은_비공개_차단_게시글을_제외한다() throws Exception {
        createPublishedPost("검색어포함 공개", "본문", PostVisibility.PUBLIC);
        createPublishedPost("검색어포함 비공개", "본문", PostVisibility.PRIVATE);
        Post blocked = createPublishedPost("검색어포함 차단", "본문", PostVisibility.PUBLIC);
        markBlocked(blocked.getPostId());

        mockMvc.perform(get("/api/community/posts/search").param("query", "검색어포함"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("검색어포함 공개"));
    }

    @Test
    void sigunguCodes가_명시적으로_비어있으면_빈_결과다() {
        // 쿼리 파라미터로는 "명시적 빈 배열"을 표준적으로 표현할 방법이 없어(생략과 구분 불가)
        // 서비스 계층에서 직접 검증한다 — 실제 단위는 CommunitySearchValidatorTest에서도 확인.
        createPublishedPost("아무거나 제목", "본문", PostVisibility.PUBLIC);

        var response = communityPostSearchService.search("아무거나", List.of(), null, null, null, null);

        org.assertj.core.api.Assertions.assertThat(response.items()).isEmpty();
        org.assertj.core.api.Assertions.assertThat(response.nextCursor()).isNull();
    }

    @Test
    void 검색_존재하지_않는_커서는_400이다() throws Exception {
        mockMvc.perform(get("/api/community/posts/search").param("query", "아무거나").param("cursor", "999999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    // ===== 공유수 증가 =====

    @Test
    void 유효한_공유는_204와_카운트_증가를_반환한다() throws Exception {
        Post target = createPublishedPost("공유대상", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/posts/{postId}/shares", target.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"KAKAO\"}"))
                .andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        assertShareCount(reloaded, 1);

        Long shareRows = ((Number) entityManager.createNativeQuery(
                        "select count(*) from post_shares where post_id = :id and channel = 'KAKAO' and user_id is null")
                .setParameter("id", target.getPostId())
                .getSingleResult()).longValue();
        org.assertj.core.api.Assertions.assertThat(shareRows).isEqualTo(1);
    }

    @Test
    void 로그인한_공유는_user_id가_기록된다() throws Exception {
        Post target = createPublishedPost("공유대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/shares", target.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"LINK\"}"))
                .andExpect(status().isNoContent());

        Long rows = ((Number) entityManager.createNativeQuery(
                        "select count(*) from post_shares where post_id = :id and user_id = :userId")
                .setParameter("id", target.getPostId())
                .setParameter("userId", OTHER_USER_ID)
                .getSingleResult()).longValue();
        org.assertj.core.api.Assertions.assertThat(rows).isEqualTo(1);
    }

    @Test
    void 잘못된_채널은_400이다() throws Exception {
        Post target = createPublishedPost("공유대상", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/posts/{postId}/shares", target.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"INSTAGRAM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SHARE_CHANNEL"));
    }

    @Test
    void 비공개_게시글은_공유할_수_없다() throws Exception {
        Post target = createPublishedPost("비공개", "본문", PostVisibility.PRIVATE);

        mockMvc.perform(post("/api/posts/{postId}/shares", target.getPostId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"KAKAO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    private void assertShareCount(Post post, long expected) {
        org.assertj.core.api.Assertions.assertThat(post.getShareCount()).isEqualTo(expected);
    }

    // ===== 신고 =====

    @Test
    void 유효한_신고는_201과_RECEIVED를_반환한다() throws Exception {
        Post target = createPublishedPost("신고대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedPostId\":" + target.getPostId() + ",\"reasonCode\":\"SPAM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void 같은_게시글을_중복_신고하면_409다() throws Exception {
        Post target = createPublishedPost("신고대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(OTHER_USER_ID);
        String body = "{\"reportedPostId\":" + target.getPostId() + ",\"reasonCode\":\"SPAM\"}";

        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reports").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_SUBMITTED"));
    }

    @Test
    void 차단된_게시글도_신고할_수_있다() throws Exception {
        Post target = createPublishedPost("차단대상", "본문", PostVisibility.PUBLIC);
        markBlocked(target.getPostId());
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedPostId\":" + target.getPostId() + ",\"reasonCode\":\"ABUSE\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 삭제된_게시글_신고는_404다() throws Exception {
        Post target = createPublishedPost("삭제대상", "본문", PostVisibility.PUBLIC);
        target.tombstone();
        postRepository.saveAndFlush(target);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedPostId\":" + target.getPostId() + ",\"reasonCode\":\"SPAM\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 비로그인_신고는_401이다() throws Exception {
        Post target = createPublishedPost("신고대상", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportedPostId\":" + target.getPostId() + ",\"reasonCode\":\"SPAM\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 카드 실제 데이터(TripPostReader/RegionReader 연동) =====

    @Test
    void 카드에_실제_지역과_작성자_userId가_채워진다() throws Exception {
        createPublishedPost(AUTHOR_USER_ID, "제목", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].region.sigunguCode").value(SIGUNGU_CODE))
                .andExpect(jsonPath("$.items[0].author.userId").value(AUTHOR_USER_ID));
    }

    // ===== FOLLOWING 탭 =====

    @Test
    void FOLLOWING_탭은_팔로우한_유저의_글만_보여준다() throws Exception {
        createPublishedPost(AUTHOR_USER_ID, "팔로우한사람글", "본문", PostVisibility.PUBLIC);
        createPublishedPost(OTHER_USER_ID, "안팔로우한사람글", "본문", PostVisibility.PUBLIC);
        follow(USER_ID, AUTHOR_USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/community/posts").param("tab", "FOLLOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("팔로우한사람글"));
    }

    @Test
    void FOLLOWING_탭_팔로우가_없으면_빈_목록이다() throws Exception {
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/community/posts").param("tab", "FOLLOWING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void FOLLOWING_탭_비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/community/posts").param("tab", "FOLLOWING"))
                .andExpect(status().isUnauthorized());
    }

    // ===== 차단 필터링 =====

    @Test
    void 차단한_유저의_글은_ALL_탭에서_제외된다() throws Exception {
        createPublishedPost(AUTHOR_USER_ID, "차단대상글", "본문", PostVisibility.PUBLIC);
        createPublishedPost(OTHER_USER_ID, "정상글", "본문", PostVisibility.PUBLIC);
        block(USER_ID, AUTHOR_USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("정상글"));
    }

    @Test
    void 나를_차단한_유저의_글도_ALL_탭에서_제외된다() throws Exception {
        createPublishedPost(AUTHOR_USER_ID, "상대방글", "본문", PostVisibility.PUBLIC);
        block(AUTHOR_USER_ID, USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/community/posts").param("tab", "ALL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 차단한_유저의_글은_검색에서도_제외된다() throws Exception {
        createPublishedPost(AUTHOR_USER_ID, "검색어포함 차단대상", "본문", PostVisibility.PUBLIC);
        createPublishedPost(OTHER_USER_ID, "검색어포함 정상글", "본문", PostVisibility.PUBLIC);
        block(USER_ID, AUTHOR_USER_ID);
        authenticateAs(USER_ID);

        mockMvc.perform(get("/api/community/posts/search").param("query", "검색어포함"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("검색어포함 정상글"));
    }

    // ===== 조회수 증가 =====

    @Test
    void 조회수_증가는_뷰_기록을_남기고_카운트를_올린다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "조회대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId()))
                .andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getViewCount()).isEqualTo(1);

        Long rows = ((Number) entityManager.createNativeQuery(
                        "select count(*) from post_view_histories where post_id = :id and viewer_user_id = :userId")
                .setParameter("id", target.getPostId())
                .setParameter("userId", OTHER_USER_ID)
                .getSingleResult()).longValue();
        org.assertj.core.api.Assertions.assertThat(rows).isEqualTo(1);
    }

    @Test
    void 작성자_본인_조회는_집계되지_않는다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "조회대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(AUTHOR_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId()))
                .andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getViewCount()).isZero();
    }

    @Test
    void 비로그인_조회는_집계되지_않는다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "조회대상", "본문", PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId()))
                .andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getViewCount()).isZero();
    }

    @Test
    void 짧은_시간_내_중복_조회는_한번만_집계된다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "조회대상", "본문", PostVisibility.PUBLIC);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId())).andExpect(status().isNoContent());
        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId())).andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getViewCount()).isEqualTo(1);
    }

    @Test
    void 접근_불가능한_게시글_조회는_404다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "비공개", "본문", PostVisibility.PRIVATE);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 차단_관계_조회는_집계되지_않지만_204다() throws Exception {
        Post target = createPublishedPost(AUTHOR_USER_ID, "조회대상", "본문", PostVisibility.PUBLIC);
        block(OTHER_USER_ID, AUTHOR_USER_ID);
        authenticateAs(OTHER_USER_ID);

        mockMvc.perform(post("/api/posts/{postId}/views", target.getPostId()))
                .andExpect(status().isNoContent());

        entityManager.clear();
        Post reloaded = postRepository.findById(target.getPostId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(reloaded.getViewCount()).isZero();
    }
}
