package com.travelbird.place;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/places/resolve} 통합 테스트. backend-functional-spec-v10.md §3.6.4 — NAVER 결과는
 * 표시 전용이라 읽기 전용으로 기존 canonical 장소의 {@code placeId}만 돌려준다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PlaceResolveIntegrationTest {

    private static final Long USER_ID = 1L;
    private static final String NAME = "경복궁";
    private static final String ADDRESS = "서울특별시 종로구 사직로 161";
    private static final String LAT = "37.5796000";
    private static final String LNG = "126.9770000";

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

    private Long insertPlace(String name, String address, String lat, String lng, String status) {
        entityManager.createNativeQuery(
                        "insert into places (status, name, category, address, sigungu_code, latitude, longitude) "
                                + "values (:status, :name, 'ATTRACTION', :address, '11110', :lat, :lng)")
                .setParameter("status", status).setParameter("name", name).setParameter("address", address)
                .setParameter("lat", new BigDecimal(lat)).setParameter("lng", new BigDecimal(lng))
                .executeUpdate();
        return ((Number) entityManager.createNativeQuery("select last_insert_id()").getSingleResult()).longValue();
    }

    private long count(String table) {
        return ((Number) entityManager.createNativeQuery("select count(*) from " + table).getSingleResult())
                .longValue();
    }

    private String body(String externalPlaceId, String name, String address, String lat, String lng) {
        return "{\"externalPlaceId\":\"" + externalPlaceId + "\",\"placeName\":\"" + name
                + "\",\"address\":\"" + address + "\",\"latitude\":" + lat + ",\"longitude\":" + lng + "}";
    }

    private org.springframework.test.web.servlet.ResultActions resolve(String json) throws Exception {
        return mockMvc.perform(post("/api/places/resolve").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void 이름_주소_좌표가_일치하는_기존_장소의_placeId를_반환한다() throws Exception {
        Long placeId = insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");

        resolve(body("N-1", NAME, ADDRESS, LAT, LNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(placeId));
    }

    @Test
    void HTML_태그와_공백_차이는_무시하고_50m_이내면_일치한다() throws Exception {
        Long placeId = insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");

        // 위도 +0.0002도는 약 22m다.
        resolve(body("N-2", "<b>경복궁</b>", "서울특별시  종로구   사직로 161", "37.5798000", LNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(placeId));
    }

    @Test
    void 이름은_같지만_주소가_다르면_404다() throws Exception {
        insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");

        resolve(body("N-3", NAME, "서울특별시 중구 세종대로 110", LAT, LNG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void 좌표가_50m를_넘게_떨어져_있으면_404다() throws Exception {
        insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");

        // 위도 +0.001도는 약 111m다.
        resolve(body("N-4", NAME, ADDRESS, "37.5806000", LNG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void 고신뢰_후보가_둘_이상이면_모호해서_404다() throws Exception {
        insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");
        insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");

        resolve(body("N-5", NAME, ADDRESS, LAT, LNG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void CLOSED_장소는_후보에서_제외한다() throws Exception {
        insertPlace(NAME, ADDRESS, LAT, LNG, "CLOSED");

        resolve(body("N-6", NAME, ADDRESS, LAT, LNG))
                .andExpect(status().isNotFound());
    }

    @Test
    void 이미_NAVER_매핑이_있으면_이름이_달라도_그_placeId를_반환한다() throws Exception {
        Long placeId = insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");
        entityManager.createNativeQuery("insert into place_external_ids (place_id, provider, external_place_id) "
                        + "values (:p, 'NAVER', 'N-7')")
                .setParameter("p", placeId).executeUpdate();

        resolve(body("N-7", "전혀 다른 이름", "다른 주소", "35.0000000", "129.0000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(placeId));
    }

    @Test
    void resolve는_장소와_외부ID를_저장하지_않는다() throws Exception {
        insertPlace(NAME, ADDRESS, LAT, LNG, "ACTIVE");
        long placesBefore = count("places");
        long externalIdsBefore = count("place_external_ids");

        resolve(body("N-8", NAME, ADDRESS, LAT, LNG)).andExpect(status().isOk());
        resolve(body("N-9", "없는 장소", "없는 주소", LAT, LNG)).andExpect(status().isNotFound());

        assertThat(count("places")).isEqualTo(placesBefore);
        assertThat(count("place_external_ids")).isEqualTo(externalIdsBefore);
    }

    @Test
    void 필수값이_빠지면_400이다() throws Exception {
        resolve("{\"externalPlaceId\":\"N-10\",\"placeName\":\"경복궁\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        resolve(body("  ", NAME, ADDRESS, LAT, LNG))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void 좌표가_범위를_벗어나면_400_INVALID_COORDINATES다() throws Exception {
        resolve(body("N-11", NAME, ADDRESS, "95.0", LNG))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_COORDINATES"));
    }
}
