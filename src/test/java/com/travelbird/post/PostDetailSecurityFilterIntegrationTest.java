package com.travelbird.post;

import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/posts/{postId}} 비로그인 접근을 실제 Spring Security 필터 체인까지
 * 포함해서 검증한다. {@code PostReadIntegrationTest}는 인증 fixture 편의를 위해
 * {@code addFilters=false}로 필터를 꺼서, PR#17의 permitAll 설정이 실제로 이 경로에
 * 적용되는지는 그 테스트로는 확인할 수 없다(chun9930 PR#22 리뷰).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostDetailSecurityFilterIntegrationTest {

    private static final String SIGUNGU_CODE = "11110";
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PostRepository postRepository;

    @Test
    @Transactional
    void 비로그인도_실제_보안_필터를_통과해_공개_게시글_상세를_조회할_수_있다() throws Exception {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID)
                .executeUpdate();
        entityManager.createNativeQuery(
                        "insert into trips (user_id, source_type, title, sigungu_code, start_date, end_date, "
                                + "companion_type, pace) values (:userId, 'MANUAL', '테스트 여행', :sigunguCode, "
                                + ":start, :end, 'SOLO', 'RELAXED')")
                .setParameter("userId", USER_ID)
                .setParameter("sigunguCode", SIGUNGU_CODE)
                .setParameter("start", LocalDate.now())
                .setParameter("end", LocalDate.now().plusDays(1))
                .executeUpdate();
        Number tripId = (Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult();

        Post post = postRepository.saveAndFlush(
                Post.create(tripId.longValue(), "제목", "본문", null, PostVisibility.PUBLIC, true));

        mockMvc.perform(get("/api/posts/{postId}", post.getPostId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("제목"));
    }
}
