package com.travelbird.place.service;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.community.service.CommunitySearchValidator;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.home.dto.response.PlaceSummary;
import com.travelbird.place.controller.dto.PlaceSearchResponse;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.domain.Place;
import com.travelbird.place.repository.PlaceSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 통합검색(§3.16.1) 장소 섹션. 내부 canonical {@code places}만 검색하므로 결과는 항상 내부
 * {@code placeId}를 갖고, NAVER 검색 결과와 달리 저장·일정 추가에 그대로 쓸 수 있다.
 * {@code externalPlaceId}는 NAVER 표시 전용 식별자라 내부 장소에서는 항상 {@code null}이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceSearchService {

    private static final int QUERY_MIN_LENGTH = 2;
    private static final int DEFAULT_DISPLAY = 15;
    private static final int MAX_DISPLAY = 30;

    private final PlaceSearchRepository placeSearchRepository;
    private final SavedPlaceReader savedPlaceReader;
    private final CommunitySearchValidator searchValidator;

    public List<PlaceSummary> search(String query, List<String> sigunguCodesOrNull, Long viewerId, int limit) {
        List<Place> places = placeSearchRepository.search(query, sigunguCodesOrNull, limit);
        if (places.isEmpty()) {
            return List.of();
        }
        Set<Long> savedPlaceIds = Set.copyOf(savedPlaceReader.getSavedPlaceIds(viewerId));
        return toSummaries(places, Set.copyOf(savedPlaceReader.getSavedPlaceIds(viewerId)));
    }

    /**
     * 장소 검색 API(§3.6.1). NAVER 호출 없이 내부 canonical {@code places}만 검색한다(NAVER 결과는 표시
     * 전용·DB 저장 불가 정책) — 모든 결과가 {@code placeId}를 갖는다. {@code start}는 1부터 시작하는
     * 결과 순번, {@code display}는 1~30으로 보정한다(스펙에 상한이 없어 통합검색 {@code limitPerType}과
     * 같은 방식으로 보정).
     */
    public PlaceSearchResponse searchPlaces(String rawQuery, String categoryRaw, BigDecimal latitude,
                                             BigDecimal longitude, Integer startOrNull, Integer displayOrNull,
                                             Long viewerId) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.isEmpty()) {
            throw new BusinessException(ErrorCode.SEARCH_QUERY_REQUIRED);
        }
        searchValidator.validateQueryLength(query);
        if (query.length() < QUERY_MIN_LENGTH) {
            throw new BusinessException(ErrorCode.SEARCH_QUERY_TOO_SHORT);
        }
        PlaceCategory category = parseCategory(categoryRaw);
        validateCoordinates(latitude, longitude);

        long offset = Math.max(startOrNull == null ? 1 : startOrNull, 1) - 1L;
        int display = displayOrNull == null ? DEFAULT_DISPLAY : Math.min(Math.max(displayOrNull, 1), MAX_DISPLAY);

        List<Place> places = placeSearchRepository.search(query, category, latitude, longitude, offset, display);
        long total = placeSearchRepository.count(query, category);
        List<PlaceSummary> items = places.isEmpty()
                ? List.of()
                : toSummaries(places, Set.copyOf(savedPlaceReader.getSavedPlaceIds(viewerId)));
        return new PlaceSearchResponse(items, (int) total, offset + items.size() >= total);
    }

    private PlaceCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PlaceCategory.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PLACE_CATEGORY);
        }
    }

    /** 좌표는 둘 다 있거나 둘 다 없어야 하고 위도 ±90·경도 ±180 안이어야 한다. */
    private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null && longitude == null) {
            return;
        }
        if (latitude == null || longitude == null
                || latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_COORDINATES);
        }
    }

    private List<PlaceSummary> toSummaries(List<Place> places, Set<Long> savedPlaceIds) {
        return places.stream()
                .map(place -> new PlaceSummary(
                        place.getPlaceId(),
                        null,
                        place.getName(),
                        place.getExternalCategory(),
                        place.getCategory(),
                        place.getAddress(),
                        place.getLatitude(),
                        place.getLongitude(),
                        savedPlaceIds.contains(place.getPlaceId())))
                .toList();
    }
}
