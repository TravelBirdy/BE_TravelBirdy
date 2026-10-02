package com.travelbird.place;

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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/places/search} 통합 테스트. backend-functional-spec-v10.md §3.6.1 — NAVER 호출
 * 없이 내부 canonical 장소만 검색한다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PlaceSearchApiIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String SIGUNGU_CODE = "11110";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID).executeUpdate();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private Long insertPlace(String name, String address, String category, String status,
                              String latitude, String longitude) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values (:status, :name, :category, :address, :sigungu, :lat, :lng)")
                .setParameter("status", status).setParameter("name", name)
                .setParameter("category", category).setParameter("address", address)
                .setParameter("sigungu", SIGUNGU_CODE)
                .setParameter("lat", new java.math.BigDecimal(latitude))
                .setParameter("lng", new java.math.BigDecimal(longitude))
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
    }

    private Long insertPlace(String name, String address, String category) {
        return insertPlace(name, address, category, "ACTIVE", "37.5700000", "126.9800000");
    }

    @Test
    void 이름이_일치하는_장소가_주소만_일치하는_장소보다_먼저_나온다() throws Exception {
        insertPlace("광화문 카페", "경복궁 앞 거리", "CAFE");
        Long nameMatch = insertPlace("경복궁", "서울 종로구 사직로", "ATTRACTION");

        mockMvc.perform(get("/api/places/search").param("query", "경복궁"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].placeId").value(nameMatch))
                .andExpect(jsonPath("$.items[0].externalPlaceId").doesNotExist())
                .andExpect(jsonPath("$.items[0].category").value("ATTRACTION"))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.isEnd").value(true));
    }

    @Test
    void category로_필터링한다() throws Exception {
        insertPlace("경복궁 카페", "서울", "CAFE");
        insertPlace("경복궁", "서울", "ATTRACTION");

        mockMvc.perform(get("/api/places/search").param("query", "경복궁").param("category", "CAFE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("경복궁 카페"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void CLOSED_장소는_제외한다() throws Exception {
        insertPlace("경복궁", "서울", "ATTRACTION", "CLOSED", "37.5700000", "126.9800000");

        mockMvc.perform(get("/api/places/search").param("query", "경복궁"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.isEnd").value(true));
    }

    @Test
    void start와_display로_페이징하고_마지막_페이지에서_isEnd가_true다() throws Exception {
        insertPlace("서울광장 1", "서울", "ATTRACTION");
        Long second = insertPlace("서울광장 2", "서울", "ATTRACTION");
        Long third = insertPlace("서울광장 3", "서울", "ATTRACTION");

        mockMvc.perform(get("/api/places/search").param("query", "서울광장")
                        .param("start", "2").param("display", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].placeId").value(second))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.isEnd").value(false));

        mockMvc.perform(get("/api/places/search").param("query", "서울광장")
                        .param("start", "3").param("display", "5"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].placeId").value(third))
                .andExpect(jsonPath("$.isEnd").value(true));
    }

    @Test
    void 저장한_장소는_saved가_true다() throws Exception {
        Long saved = insertPlace("경복궁", "서울", "ATTRACTION");
        insertPlace("경복궁 카페", "서울", "CAFE");
        entityManager.createNativeQuery("insert into saved_places (user_id, place_id) values (:u, :p)")
                .setParameter("u", USER_ID).setParameter("p", saved).executeUpdate();

        mockMvc.perform(get("/api/places/search").param("query", "경복궁"))
                .andExpect(jsonPath("$.items[0].placeId").value(saved))
                .andExpect(jsonPath("$.items[0].saved").value(true))
                .andExpect(jsonPath("$.items[1].saved").value(false));
    }

    @Test
    void 좌표를_주면_같은_일치_단계_안에서_가까운_장소가_먼저_나온다() throws Exception {
        // 이름순으로는 A가 앞이지만 기준 좌표는 B에 훨씬 가깝다.
        insertPlace("남산타워 A", "서울", "ATTRACTION", "ACTIVE", "37.6500000", "127.0500000");
        Long near = insertPlace("남산타워 B", "서울", "ATTRACTION", "ACTIVE", "37.5512000", "126.9882000");

        mockMvc.perform(get("/api/places/search").param("query", "남산타워")
                        .param("latitude", "37.5511").param("longitude", "126.9881"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].placeId").value(near));
    }

    @Test
    void 검색어가_없으면_400_SEARCH_QUERY_REQUIRED다() throws Exception {
        mockMvc.perform(get("/api/places/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_REQUIRED"));
        mockMvc.perform(get("/api/places/search").param("query", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_REQUIRED"));
    }

    @Test
    void 검색어가_2자_미만이면_400_SEARCH_QUERY_TOO_SHORT다() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "궁"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_TOO_SHORT"));
    }

    @Test
    void 검색어가_50자를_넘으면_400_SEARCH_QUERY_TOO_LONG이다() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "가".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_QUERY_TOO_LONG"));
    }

    @Test
    void 잘못된_category는_400_INVALID_PLACE_CATEGORY다() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "경복궁").param("category", "PHOTO_SPOT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLACE_CATEGORY"));
    }

    @Test
    void 좌표가_한쪽만_있거나_범위를_벗어나면_400_INVALID_COORDINATES다() throws Exception {
        mockMvc.perform(get("/api/places/search").param("query", "경복궁").param("latitude", "37.5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
        mockMvc.perform(get("/api/places/search").param("query", "경복궁")
                        .param("latitude", "91").param("longitude", "126.9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }

    @Test
    void 검색어의_LIKE_특수문자는_이스케이프된다() throws Exception {
        insertPlace("경복궁", "서울", "ATTRACTION");

        mockMvc.perform(get("/api/places/search").param("query", "%%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
