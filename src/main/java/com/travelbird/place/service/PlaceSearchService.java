package com.travelbird.place.service;

import com.travelbird.home.dto.response.PlaceSummary;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.domain.Place;
import com.travelbird.place.repository.PlaceSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final PlaceSearchRepository placeSearchRepository;
    private final SavedPlaceReader savedPlaceReader;

    public List<PlaceSummary> search(String query, List<String> sigunguCodesOrNull, Long viewerId, int limit) {
        List<Place> places = placeSearchRepository.search(query, sigunguCodesOrNull, limit);
        if (places.isEmpty()) {
            return List.of();
        }
        Set<Long> savedPlaceIds = Set.copyOf(savedPlaceReader.getSavedPlaceIds(viewerId));
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
