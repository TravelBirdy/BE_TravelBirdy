package com.travelbird.place.service;

import com.travelbird.place.domain.Place;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * "같은 장소로 볼 수 있는가"의 보수적 판정 — 정규화 장소명 일치 AND 정규화 주소 일치 AND 좌표 50m
 * 이내이고 후보가 정확히 1개일 때만 인정한다(공통협의 9절, backend-functional-spec-v10.md §3.6.4).
 * TourAPI Import 병합({@code TourApiPlaceImportService})과 {@code /api/places/resolve}가
 * 같은 기준을 쓰도록 공유한다.
 */
@Component
public class PlaceMatcher {

    private static final double MERGE_RADIUS_METERS = 50.0;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** 고신뢰 후보가 정확히 1개면 그 장소를, 없거나 여러 개면(모호) 비어 있는 값을 돌려준다. */
    public Optional<Place> findUniqueMatch(List<Place> candidates, String name, String address,
                                            BigDecimal latitude, BigDecimal longitude) {
        String normalizedName = normalizeName(name);
        String normalizedAddress = normalizeAddress(address);

        List<Place> matches = candidates.stream()
                .filter(candidate -> normalizeName(candidate.getName()).equals(normalizedName))
                .filter(candidate -> normalizeAddress(candidate.getAddress()).equals(normalizedAddress))
                .filter(candidate -> distanceMeters(candidate.getLatitude(), candidate.getLongitude(),
                        latitude, longitude) <= MERGE_RADIUS_METERS)
                .toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    /** HTML 태그 제거 후 공백을 모두 없앤다(장소명 비교용). */
    String normalizeName(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").trim().replaceAll("\\s+", "");
    }

    String normalizeAddress(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }

    /** Haversine — 두 좌표 간 거리(m). */
    double distanceMeters(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
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
