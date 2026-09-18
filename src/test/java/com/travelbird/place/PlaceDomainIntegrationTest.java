package com.travelbird.place;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceStatus;
import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.repository.PlaceCategoryMappingRuleRepository;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.place.repository.SavedPlaceRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class PlaceDomainIntegrationTest {

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
    private EntityManager entityManager;
    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private SavedPlaceRepository savedPlaceRepository;
    @Autowired
    private PlaceCategoryMappingRuleRepository placeCategoryMappingRuleRepository;
    @Autowired
    private PlaceReader placeReader;
    @Autowired
    private SavedPlaceReader savedPlaceReader;

    private static final Long VIEWER_USER_ID = 1L;

    @BeforeEach
    void seedFixtures() {
        // region 도메인(PR1)이 아직 이 브랜치에 없어 sigungu_master 테스트용 행은 네이티브
        // SQL로 직접 넣는다 — FK(places.sigungu_code) 제약 통과용, 실제 269개 seed와 무관.
        entityManager.createNativeQuery(
                        "insert into sigungu_master (sigungu_code, sigungu_name) values (:code, :name)")
                .setParameter("code", SIGUNGU_CODE)
                .setParameter("name", "종로구")
                .executeUpdate();
        // saved_places.user_id -> users.user_id FK 통과용 — user 도메인(PR2 범위 밖)도 없어
        // 네이티브 SQL로 최소 행만 넣는다.
        entityManager.createNativeQuery(
                        "insert into users (user_id, role) values (:id, 'ROLE_USER')")
                .setParameter("id", VIEWER_USER_ID)
                .executeUpdate();
    }

    private Place persistPlace(String name) {
        Place place = Place.createFromImport(name, null, "음식점", PlaceCategory.FOOD,
                "서울 종로구 어딘가", SIGUNGU_CODE, new BigDecimal("37.5700000"), new BigDecimal("126.9800000"), null);
        return placeRepository.save(place);
    }

    @Test
    void 카테고리매핑규칙_시드가_스펙대로_25개_들어간다() {
        List<com.travelbird.place.domain.PlaceCategoryMappingRule> rules =
                placeCategoryMappingRuleRepository.findAllByOrderByPriorityAsc();

        assertThat(rules).hasSize(25);
        assertThat(rules.get(0).getIncludeKeyword()).isEqualTo("카페");
        assertThat(rules.get(0).getTargetCategory()).isEqualTo(PlaceCategory.CAFE);
        assertThat(rules.get(rules.size() - 1).getIncludeKeyword()).isEqualTo("공원");
        assertThat(rules.get(rules.size() - 1).getTargetCategory()).isEqualTo(PlaceCategory.ATTRACTION);
    }

    @Test
    void getPlace는_존재하지_않는_ID면_예외를_던진다() {
        assertThat(placeReader.existsPlace(999_999L)).isFalse();
        org.junit.jupiter.api.Assertions.assertThrows(
                com.travelbird.global.error.BusinessException.class,
                () -> placeReader.getPlace(999_999L, null));
    }

    @Test
    void getPlaces는_존재하지_않는_ID를_결과에서_제외한다() {
        Place saved = persistPlace("테스트 장소");

        List<PlaceContract> result = placeReader.getPlaces(List.of(saved.getPlaceId(), 999_999L), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).placeId()).isEqualTo(saved.getPlaceId());
        assertThat(result.get(0).saved()).isFalse();
    }

    @Test
    void saved_플래그는_viewer가_저장한_경우에만_true다() {
        Place saved = persistPlace("저장 테스트 장소");
        savedPlaceRepository.save(SavedPlace.of(VIEWER_USER_ID, saved.getPlaceId(), "메모"));

        PlaceContract withViewer = placeReader.getPlace(saved.getPlaceId(), VIEWER_USER_ID);
        PlaceContract anonymous = placeReader.getPlace(saved.getPlaceId(), null);

        assertThat(withViewer.saved()).isTrue();
        assertThat(anonymous.saved()).isFalse();
    }

    @Test
    void areAllSavedByUser는_일부만_저장된_경우_false다() {
        Place place1 = persistPlace("장소1");
        Place place2 = persistPlace("장소2");
        savedPlaceRepository.save(SavedPlace.of(VIEWER_USER_ID, place1.getPlaceId(), null));
        // place2는 저장 안 함

        boolean allSaved = savedPlaceReader.areAllSavedByUser(VIEWER_USER_ID, List.of(place1.getPlaceId(), place2.getPlaceId()));
        boolean onlyFirstSaved = savedPlaceReader.areAllSavedByUser(VIEWER_USER_ID, List.of(place1.getPlaceId()));

        assertThat(allSaved).isFalse();
        assertThat(onlyFirstSaved).isTrue();
    }

    @Test
    void 장소_상태가_CLOSED여도_예외_없이_조회된다() {
        Place place = persistPlace("폐업 장소");
        entityManager.createNativeQuery("update places set status = 'CLOSED' where place_id = :id")
                .setParameter("id", place.getPlaceId())
                .executeUpdate();
        entityManager.clear();

        PlaceContract contract = placeReader.getPlace(place.getPlaceId(), null);

        assertThat(contract.status()).isEqualTo(PlaceStatus.CLOSED);
    }
}
