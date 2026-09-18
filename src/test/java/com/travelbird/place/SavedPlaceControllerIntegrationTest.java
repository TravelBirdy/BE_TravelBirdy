package com.travelbird.place;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.Place;
import com.travelbird.place.repository.PlaceRepository;
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

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * addFilters=false로 Security 필터 체인을 끄고, {@code SecurityContextHolder}에 직접
 * 인증 정보를 채워 컨트롤러/서비스 로직만 검증한다 — 실제 로그인(JWT)은 B4 몫.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class SavedPlaceControllerIntegrationTest {

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

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlaceRepository placeRepository;

    @BeforeEach
    void setUpFixturesAndAuth() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        entityManager.createNativeQuery("insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", USER_ID)
                .executeUpdate();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private Place persistPlace(String name) {
        Place place = Place.createFromImport(name, null, "관광", PlaceCategory.ATTRACTION,
                "서울 종로구 어딘가", SIGUNGU_CODE, new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), null);
        return placeRepository.save(place);
    }

    @Test
    void 존재하는_장소를_저장하면_204를_반환하고_목록에_나타난다() throws Exception {
        Place place = persistPlace("경복궁");

        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"가보고 싶은 곳\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/saved-places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].placeId").value(place.getPlaceId()))
                .andExpect(jsonPath("$.items[0].memo").value("가보고 싶은 곳"))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void 존재하지_않는_장소_저장은_404다() throws Exception {
        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void memo가_100자를_넘으면_400이다() throws Exception {
        Place place = persistPlace("장소");
        String longMemo = "가".repeat(101);

        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"" + longMemo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_MEMO_TOO_LONG"));
    }

    @Test
    void 같은_장소를_두번_저장해도_멱등하고_메모만_갱신된다() throws Exception {
        Place place = persistPlace("장소");

        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"첫 메모\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"수정된 메모\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/saved-places"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].memo").value("수정된 메모"));
    }

    @Test
    void 저장취소는_저장돼있지_않아도_204다() throws Exception {
        mockMvc.perform(delete("/api/users/me/saved-places/{placeId}", 999_999L))
                .andExpect(status().isNoContent());
    }

    @Test
    void 저장취소_후_메모도_같이_사라진다() throws Exception {
        Place place = persistPlace("장소");
        mockMvc.perform(put("/api/users/me/saved-places/{placeId}", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"메모\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/me/saved-places/{placeId}", place.getPlaceId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/me/saved-places"))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void 저장하지_않은_장소의_메모수정은_404다() throws Exception {
        Place place = persistPlace("장소");

        mockMvc.perform(patch("/api/users/me/saved-places/{placeId}/memo", place.getPlaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memo\":\"메모\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAVED_PLACE_NOT_FOUND"));
    }

    @Test
    void 목록은_저장일_내림차순_커서페이지네이션이다() throws Exception {
        Place place1 = persistPlace("장소1");
        Place place2 = persistPlace("장소2");
        Place place3 = persistPlace("장소3");
        for (Place p : List.of(place1, place2, place3)) {
            mockMvc.perform(put("/api/users/me/saved-places/{placeId}", p.getPlaceId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNoContent());
        }

        mockMvc.perform(get("/api/users/me/saved-places").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].placeId").value(place3.getPlaceId()))
                .andExpect(jsonPath("$.items[1].placeId").value(place2.getPlaceId()))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.nextCursor").value(place2.getPlaceId()));

        mockMvc.perform(get("/api/users/me/saved-places")
                        .param("size", "2")
                        .param("cursor", String.valueOf(place2.getPlaceId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].placeId").value(place1.getPlaceId()))
                .andExpect(jsonPath("$.hasNext").value(false));
    }
}
