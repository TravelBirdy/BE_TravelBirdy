package com.travelbird.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code DELETE /api/posts/{postId}} 비로그인 접근을 실제 Spring Security 필터 체인까지
 * 포함해서 검증한다(§3.8.4). {@code GET /api/posts/*}만 permitAll이고 DELETE는 메서드
 * 단위로 제외돼 있어(SecurityConfig) 인증이 필요하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostDeleteSecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 비로그인_삭제_요청은_401이다() throws Exception {
        mockMvc.perform(delete("/api/posts/{postId}", 1L))
                .andExpect(status().isUnauthorized());
    }
}
