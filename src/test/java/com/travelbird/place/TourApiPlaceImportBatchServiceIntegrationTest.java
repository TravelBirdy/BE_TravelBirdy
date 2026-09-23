package com.travelbird.place;

import com.travelbird.place.repository.PlaceExternalIdRepository;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.place.service.TourApiImportReport;
import com.travelbird.place.service.TourApiPlaceImportBatchService;
import com.travelbird.place.service.TourApiPlaceImportRow;
import jakarta.persistence.EntityManager;
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
class TourApiPlaceImportBatchServiceIntegrationTest {

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
    private TourApiPlaceImportBatchService tourApiPlaceImportBatchService;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PlaceExternalIdRepository placeExternalIdRepository;
    @Autowired
    private EntityManager entityManager;

    private TourApiPlaceImportRow row(String externalId, String name, String regionCode) {
        return new TourApiPlaceImportRow(externalId, name, "서울 종로구 어딘가", "관광",
                new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), regionCode, null, null);
    }

    @Test
    void 전부_성공하면_totalCount_successCount가_같고_실패는_없다() {
        TourApiImportReport report = tourApiPlaceImportBatchService.importAll(List.of(
                row("100", "경복궁", SIGUNGU_CODE),
                row("200", "남산타워", SIGUNGU_CODE)));

        assertThat(report.totalCount()).isEqualTo(2);
        assertThat(report.successCount()).isEqualTo(2);
        assertThat(report.failureCount()).isZero();
        assertThat(report.mappings()).hasSize(2);
        assertThat(report.failures()).isEmpty();
    }

    @Test
    void 존재하지_않는_지역코드는_실패로_기록되고_나머지_행_처리는_계속된다() {
        TourApiImportReport report = tourApiPlaceImportBatchService.importAll(List.of(
                row("100", "경복궁", SIGUNGU_CODE),
                row("999", "존재안하는지역장소", "99999"),
                row("200", "남산타워", SIGUNGU_CODE)));

        assertThat(report.totalCount()).isEqualTo(3);
        assertThat(report.successCount()).isEqualTo(2);
        assertThat(report.failureCount()).isEqualTo(1);
        assertThat(report.failures()).hasSize(1);
        assertThat(report.failures().get(0).externalPlaceId()).isEqualTo("999");
        assertThat(report.mappings()).extracting("externalPlaceId")
                .containsExactlyInAnyOrder("100", "200");
        assertThat(placeExternalIdRepository.findAll()).hasSize(2);
    }

    @Test
    @Transactional
    void 카테고리_제외_규칙에_매치되면_실패_목록에_사유와_함께_남는다() {
        entityManager.createNativeQuery(
                        "insert into place_category_mapping_rules (priority, include_keyword, target_category, excluded) "
                                + "values (9999, '병원', 'OTHER', true)")
                .executeUpdate();

        TourApiImportReport report = tourApiPlaceImportBatchService.importAll(List.of(
                new TourApiPlaceImportRow("100", "종로병원", "서울 종로구 어딘가", "병원",
                        new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), SIGUNGU_CODE, null, null)));

        assertThat(report.totalCount()).isEqualTo(1);
        assertThat(report.successCount()).isZero();
        assertThat(report.failureCount()).isEqualTo(1);
        assertThat(report.failures().get(0).externalPlaceId()).isEqualTo("100");
        assertThat(report.failures().get(0).reason()).contains("제외");
        assertThat(placeRepository.findAll()).isEmpty();
    }
}
