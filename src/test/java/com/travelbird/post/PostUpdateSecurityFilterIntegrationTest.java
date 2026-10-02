package com.travelbird.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PATCH /api/posts/{postId}} 비로그인 접근을 실제 Spring Security 필터 체인까지
 * 포함해서 검증한다(§3.8.4). {@code GET /api/posts/*}만 permitAll이고 PATCH는 인증이
 * 필요하다(SecurityConfig).
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostUpdateSecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 비로그인_수정_요청은_401이다() throws Exception {
        mockMvc.perform(patch("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"version\":0}"))
                .andExpect(status().isUnauthorized());
    }
}
