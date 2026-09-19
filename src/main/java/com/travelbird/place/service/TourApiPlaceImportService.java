package com.travelbird.place.service;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceExternalId;
import com.travelbird.place.domain.PlaceExternalIdProvider;
import com.travelbird.place.repository.PlaceExternalIdRepository;
import com.travelbird.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * TourAPI 초기 Import(Seeder). backend-functional-spec-v10.md §3.6.5.
 *
 * <p>재실행 멱등: {@code (KTO_TOUR_API, externalPlaceId)}가 이미 있으면 건너뛴다.
 * 신규 externalPlaceId라도 이미 canonical화된 장소와 같은 곳으로 보이면(정규화 장소명 AND
 * 주소 일치, 좌표 50m 이내) 새 Place를 만들지 않고 기존 placeId에 외부ID만 연결한다
 * ({@code /resolve}와 동일한 보수적 병합 기준, 공통협의 9절).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TourApiPlaceImportService {

    private static final double MERGE_RADIUS_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final PlaceRepository placeRepository;
    private final PlaceExternalIdRepository placeExternalIdRepository;
    private final PlaceCategoryMapper placeCategoryMapper;

    public List<PlaceExternalIdMapping> importPlaces(List<TourApiPlaceImportRow> rows) {
        List<PlaceExternalIdMapping> mappings = new ArrayList<>();
        for (TourApiPlaceImportRow row : rows) {
            importOne(row).ifPresent(mappings::add);
        }
        return mappings;
    }

    private Optional<PlaceExternalIdMapping> importOne(TourApiPlaceImportRow row) {
        Optional<PlaceExternalId> alreadyImported = placeExternalIdRepository
                .findByProviderAndExternalPlaceId(PlaceExternalIdProvider.KTO_TOUR_API, row.externalPlaceId());
        if (alreadyImported.isPresent()) {
            return Optional.of(toMapping(row, alreadyImported.get().getPlaceId()));
        }

        Optional<PlaceCategory> category = placeCategoryMapper.categorize(row.rawCategory());
        if (category.isEmpty()) {
            // 병원/학원/행정기관 등 제외 규칙 매치 — canonical 장소로 Import하지 않는다.
            return Optional.empty();
        }

        Long placeId = findMergeCandidate(row)
                .orElseGet(() -> createNewPlace(row, category.get()).getPlaceId());

        placeExternalIdRepository.save(
                PlaceExternalId.of(placeId, PlaceExternalIdProvider.KTO_TOUR_API, row.externalPlaceId()));
        return Optional.of(toMapping(row, placeId));
    }

    /**
     * 고신뢰 병합 후보(정규화 이름+주소 일치 AND 50m 이내)가 정확히 1개일 때만 그 placeId를
     * 반환한다. 2개 이상이면 어느 쪽이 진짜인지 판단할 수 없어 잘못된 병합을 하느니 새 Place를
     * 만드는 쪽을 택한다(chun9930 리뷰, PR#10 — {@code .findFirst()}로 아무거나 고르던 것 수정).
     */
    private Optional<Long> findMergeCandidate(TourApiPlaceImportRow row) {
        String normalizedName = normalize(row.name());
        String normalizedAddress = normalize(row.address());

        List<Place> candidates = placeRepository.findAllBySigunguCode(row.regionCode()).stream()
                .filter(candidate -> normalize(candidate.getName()).equals(normalizedName))
                .filter(candidate -> normalize(candidate.getAddress()).equals(normalizedAddress))
                .filter(candidate -> distanceMeters(candidate.getLatitude(), candidate.getLongitude(),
                        row.latitude(), row.longitude()) <= MERGE_RADIUS_METERS)
                .toList();

        return candidates.size() == 1 ? Optional.of(candidates.get(0).getPlaceId()) : Optional.empty();
    }

    private Place createNewPlace(TourApiPlaceImportRow row, PlaceCategory category) {
        Place place = Place.createFromImport(row.name(), row.description(), row.rawCategory(), category,
                row.address(), row.regionCode(), row.latitude(), row.longitude(), row.imageUrl());
        return placeRepository.save(place);
    }

    private PlaceExternalIdMapping toMapping(TourApiPlaceImportRow row, Long placeId) {
        return new PlaceExternalIdMapping(PlaceExternalIdProvider.KTO_TOUR_API, row.externalPlaceId(), placeId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    /** Haversine — 두 좌표 간 거리(m). */
    private double distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double deltaPhi = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double deltaLambda = Math.toRadians(lng2.subtract(lng1).doubleValue());

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
