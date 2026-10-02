package com.travelbird.place;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 장소 검색·resolve는 실제 Spring Security 필터 체인에서 비로그인이면 401이다(§3.6.1). */
@SpringBootTest
@AutoConfigureMockMvc
class PlaceSearchSecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 비로그인_검색은_401이다() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "경복궁"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비로그인_resolve는_401이다() throws Exception {
        mockMvc.perform(post("/api/places/resolve").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"externalPlaceId\":\"N-1\"}"))
                .andExpect(status().isUnauthorized());
    }
}
