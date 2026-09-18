package com.travelbird.place;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.place.repository.SavedPlaceRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code addFilters = false}로 Security 필터 체인을 끄고 컨트롤러/서비스 로직만 검증한다.
 * 실제 인증(로그인 시 viewerId 채워지는 것)은 kakao-login-eunjin 브랜치(B4) merge 후
 * 별도로 검증해야 한다 — 이 테스트는 "비로그인(viewerId=null)" 경로만 확인한다.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class PlaceControllerIntegrationTest {

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

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private SavedPlaceRepository savedPlaceRepository;

    @BeforeEach
    void seedSigungu() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
    }

    @Test
    void 존재하는_장소는_지역정보와_함께_200을_반환한다() throws Exception {
        Place place = placeRepository.save(Place.createFromImport(
                "경복궁", null, "고궁", PlaceCategory.ATTRACTION, "서울 종로구 사직로 161",
                SIGUNGU_CODE, new BigDecimal("37.5796000"), new BigDecimal("126.9770000"), null));

        mockMvc.perform(get("/api/places/{placeId}", place.getPlaceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeId").value(place.getPlaceId()))
                .andExpect(jsonPath("$.name").value("경복궁"))
                .andExpect(jsonPath("$.region.sigunguCode").value(SIGUNGU_CODE))
                .andExpect(jsonPath("$.region.sigunguName").value("종로구"))
                .andExpect(jsonPath("$.saved").value(false));
    }

    @Test
    void 존재하지_않는_장소는_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/places/{placeId}", 999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    void 폐업_장소도_예외없이_CLOSED_상태로_200을_반환한다() throws Exception {
        Place place = placeRepository.save(Place.createFromImport(
                "폐업식당", null, "음식점", PlaceCategory.FOOD, "서울 종로구 어딘가",
                SIGUNGU_CODE, new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), null));
        entityManager.createNativeQuery("update places set status = 'CLOSED' where place_id = :id")
                .setParameter("id", place.getPlaceId())
                .executeUpdate();
        entityManager.clear();

        mockMvc.perform(get("/api/places/{placeId}", place.getPlaceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
