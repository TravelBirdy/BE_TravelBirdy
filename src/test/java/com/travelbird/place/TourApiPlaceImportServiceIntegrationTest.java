package com.travelbird.place;

import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceCategoryMappingRule;
import com.travelbird.place.repository.PlaceExternalIdRepository;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.place.service.PlaceExternalIdMapping;
import com.travelbird.place.service.TourApiPlaceImportRow;
import com.travelbird.place.service.TourApiPlaceImportService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class TourApiPlaceImportServiceIntegrationTest {

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
    private TourApiPlaceImportService tourApiPlaceImportService;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PlaceExternalIdRepository placeExternalIdRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void seedSigungu() {
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
    }

    private TourApiPlaceImportRow row(String externalId, String name, String address,
                                       BigDecimal lat, BigDecimal lng) {
        return new TourApiPlaceImportRow(externalId, name, address, "관광", lat, lng, SIGUNGU_CODE, null, null);
    }

    @Test
    void 신규_장소는_새로_생성되고_외부ID가_연결된다() {
        List<PlaceExternalIdMapping> mappings = tourApiPlaceImportService.importPlaces(List.of(
                row("100", "경복궁", "서울 종로구 사직로 161", new BigDecimal("37.5796000"), new BigDecimal("126.9770000"))));

        assertThat(mappings).hasSize(1);
        Long placeId = mappings.get(0).placeId();
        assertThat(placeRepository.existsById(placeId)).isTrue();
        assertThat(placeExternalIdRepository.findByProviderAndExternalPlaceId(
                com.travelbird.place.domain.PlaceExternalIdProvider.KTO_TOUR_API, "100"))
                .isPresent().get().extracting("placeId").isEqualTo(placeId);
    }

    @Test
    void 같은_externalPlaceId_재수입은_멱등하다() {
        TourApiPlaceImportRow importRow = row("100", "경복궁", "서울 종로구 사직로 161",
                new BigDecimal("37.5796000"), new BigDecimal("126.9770000"));

        Long firstPlaceId = tourApiPlaceImportService.importPlaces(List.of(importRow)).get(0).placeId();
        Long secondPlaceId = tourApiPlaceImportService.importPlaces(List.of(importRow)).get(0).placeId();

        assertThat(secondPlaceId).isEqualTo(firstPlaceId);
        assertThat(placeRepository.findAll()).hasSize(1);
    }

    @Test
    void 이름_주소_일치하고_50m_이내면_기존_장소에_외부ID만_연결한다() {
        Long firstPlaceId = tourApiPlaceImportService.importPlaces(List.of(
                row("100", "경복궁", "서울 종로구 사직로 161", new BigDecimal("37.5796000"), new BigDecimal("126.9770000"))
        )).get(0).placeId();

        // 위도 0.0003도 차이 ≈ 33m — 병합 대상.
        Long secondPlaceId = tourApiPlaceImportService.importPlaces(List.of(
                row("200", "경복궁", "서울 종로구 사직로 161", new BigDecimal("37.5799000"), new BigDecimal("126.9770000"))
        )).get(0).placeId();

        assertThat(secondPlaceId).isEqualTo(firstPlaceId);
        assertThat(placeRepository.findAll()).hasSize(1);
        assertThat(placeExternalIdRepository.findAll()).hasSize(2);
    }

    @Test
    void 고신뢰_후보가_2개면_병합하지_않고_새로_생성한다() {
        // 이름/주소가 같은 기존 장소 2개를 서로 가까이(둘 다 새 row의 50m 이내) 미리 만들어둔다.
        Place existingA = placeRepository.save(Place.createFromImport("중복이름", null, "관광",
                com.travelbird.common.enums.PlaceCategory.ATTRACTION, "서울 종로구 사직로 161",
                SIGUNGU_CODE, new BigDecimal("37.5796000"), new BigDecimal("126.9770000"), null));
        Place existingB = placeRepository.save(Place.createFromImport("중복이름", null, "관광",
                com.travelbird.common.enums.PlaceCategory.ATTRACTION, "서울 종로구 사직로 161",
                SIGUNGU_CODE, new BigDecimal("37.5796200"), new BigDecimal("126.9770000"), null));

        Long importedPlaceId = tourApiPlaceImportService.importPlaces(List.of(
                row("300", "중복이름", "서울 종로구 사직로 161", new BigDecimal("37.5796100"), new BigDecimal("126.9770000"))
        )).get(0).placeId();

        assertThat(importedPlaceId).isNotEqualTo(existingA.getPlaceId());
        assertThat(importedPlaceId).isNotEqualTo(existingB.getPlaceId());
        assertThat(placeRepository.findAll()).hasSize(3);
    }

    @Test
    void 이름_주소_같아도_50m_넘으면_별도_장소로_생성한다() {
        Long firstPlaceId = tourApiPlaceImportService.importPlaces(List.of(
                row("100", "경복궁", "서울 종로구 사직로 161", new BigDecimal("37.5796000"), new BigDecimal("126.9770000"))
        )).get(0).placeId();

        // 위도 0.003도 차이 ≈ 334m — 병합 대상 아님.
        Long secondPlaceId = tourApiPlaceImportService.importPlaces(List.of(
                row("200", "경복궁", "서울 종로구 사직로 161", new BigDecimal("37.5826000"), new BigDecimal("126.9770000"))
        )).get(0).placeId();

        assertThat(secondPlaceId).isNotEqualTo(firstPlaceId);
        assertThat(placeRepository.findAll()).hasSize(2);
    }

    @Test
    void 제외_규칙에_매치되면_import하지_않는다() {
        entityManager.createNativeQuery(
                        "insert into place_category_mapping_rules (priority, include_keyword, target_category, excluded) "
                                + "values (9999, '병원', 'OTHER', true)")
                .executeUpdate();

        List<PlaceExternalIdMapping> mappings = tourApiPlaceImportService.importPlaces(List.of(
                new TourApiPlaceImportRow("100", "종로병원", "서울 종로구 어딘가", "병원",
                        new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), SIGUNGU_CODE, null, null)));

        assertThat(mappings).isEmpty();
        assertThat(placeRepository.findAll()).isEmpty();
    }

    @Test
    void 매칭되는_규칙이_없으면_OTHER로_분류한다() {
        List<PlaceExternalIdMapping> mappings = tourApiPlaceImportService.importPlaces(List.of(
                new TourApiPlaceImportRow("100", "이름모를장소", "서울 종로구 어딘가", "분류불명",
                        new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), SIGUNGU_CODE, null, null)));

        assertThat(mappings).hasSize(1);
        Long placeId = mappings.get(0).placeId();
        assertThat(placeRepository.findById(placeId).orElseThrow().getCategory())
                .isEqualTo(com.travelbird.common.enums.PlaceCategory.OTHER);
    }
}
