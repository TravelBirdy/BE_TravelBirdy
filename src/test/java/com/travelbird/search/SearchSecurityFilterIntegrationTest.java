package com.travelbird.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 통합검색은 로그인 필수 — 실제 보안 필터 체인에서 비로그인이 401인지 확인한다(§3.16.1). */
@SpringBootTest
@AutoConfigureMockMvc
class SearchSecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 비로그인은_401이다() throws Exception {
        mockMvc.perform(get("/api/search").param("query", "종로"))
                .andExpect(status().isUnauthorized());
    }
}
