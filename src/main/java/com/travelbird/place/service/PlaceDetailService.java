package com.travelbird.place.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.controller.dto.PlaceDetailResponse;
import com.travelbird.place.domain.Place;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.region.api.RegionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@code GET /api/places/{placeId}} 전용 서비스. {@code place.api.PlaceReader}(cross-part
 * 계약)는 {@code externalCategory}/{@code imageUrl}/지역명을 포함하지 않아 이 응답을 다
 * 채우지 못한다 — 같은 파트(Part 3) 소유 데이터라 Repository/타 계약을 직접 조합한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceDetailService {

    private final PlaceRepository placeRepository;
    private final RegionReader regionReader;
    private final SavedPlaceReader savedPlaceReader;

    public PlaceDetailResponse getPlaceDetail(Long placeId, Long viewerIdOrNull) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        RegionSummary region = regionReader.getRegion(place.getSigunguCode());

        boolean saved = viewerIdOrNull != null
                && savedPlaceReader.areAllSavedByUser(viewerIdOrNull, List.of(placeId));

        return PlaceDetailResponse.of(place, region, saved);
    }
}
