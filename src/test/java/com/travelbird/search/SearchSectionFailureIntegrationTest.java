package com.travelbird.search;

import com.travelbird.place.service.PlaceSearchService;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 한 섹션이 실패해도 나머지 섹션 결과는 부분 반환되고 실패한 섹션만 unavailableSections에 담긴다. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SearchSectionFailureIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PlaceSearchService placeSearchService;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 장소_섹션이_실패해도_지역_결과는_부분_반환된다() throws Exception {
        Mockito.when(placeSearchService.search(Mockito.anyString(), Mockito.any(), Mockito.anyLong(), Mockito.anyInt()))
                .thenThrow(new IllegalStateException("장소 조회 실패"));

        mockMvc.perform(get("/api/search").param("query", "종로"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.places.length()").value(0))
                .andExpect(jsonPath("$.regions.length()").value(1))
                .andExpect(jsonPath("$.unavailableSections.length()").value(1))
                .andExpect(jsonPath("$.unavailableSections[0]").value("PLACES"));
    }
}
