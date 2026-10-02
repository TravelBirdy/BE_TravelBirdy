package com.travelbird.place.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.controller.dto.ResolvePlaceRequest;
import com.travelbird.place.controller.dto.ResolvePlaceResponse;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceExternalIdProvider;
import com.travelbird.place.domain.PlaceStatus;
import com.travelbird.place.repository.PlaceExternalIdRepository;
import com.travelbird.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 외부(NAVER) 장소 → 내부 {@code placeId} 확정. backend-functional-spec-v10.md §3.6.4.
 *
 * <p>NAVER 검색 결과는 표시 전용이라 DB에 저장하지 않는 정책(2026-09 약관 개정)에 따라 <b>읽기 전용</b>이다.
 * 스펙의 "기존 canonical에 NAVER 외부 ID 연결"과 "신규 장소 생성"은 하지 않는다:
 * <ol>
 *   <li>{@code (NAVER, externalPlaceId)} 매핑이 이미 있으면 그 {@code placeId}를 반환한다.</li>
 *   <li>없으면 정규화 장소명·주소 일치 AND 좌표 50m 이내인 고신뢰 후보가 정확히 1개일 때만 그
 *       {@code placeId}를 반환한다({@link PlaceMatcher}, TourAPI Import 병합과 같은 기준).</li>
 *   <li>그 외(후보 없음·복수)는 {@code 404 PLACE_NOT_FOUND}다 — DB에 없는 NAVER 장소를 여행에
 *       추가하는 흐름은 이번 범위에서 보류됐다.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceResolveService {

    /** 후보 조회용 좌표 bounding box 반폭(도). 50m 판정보다 넉넉한 약 100m다. */
    private static final BigDecimal BOX_HALF_WIDTH = new BigDecimal("0.001");

    private final PlaceExternalIdRepository placeExternalIdRepository;
    private final PlaceRepository placeRepository;
    private final PlaceMatcher placeMatcher;

    public ResolvePlaceResponse resolve(ResolvePlaceRequest request) {
        validate(request);

        return placeExternalIdRepository
                .findByProviderAndExternalPlaceId(PlaceExternalIdProvider.NAVER, request.externalPlaceId().trim())
                .map(mapped -> new ResolvePlaceResponse(mapped.getPlaceId()))
                .orElseGet(() -> new ResolvePlaceResponse(findMatchingPlaceId(request)));
    }

    private Long findMatchingPlaceId(ResolvePlaceRequest request) {
        List<Place> candidates = placeRepository.findByLatitudeBetweenAndLongitudeBetween(
                        request.latitude().subtract(BOX_HALF_WIDTH), request.latitude().add(BOX_HALF_WIDTH),
                        request.longitude().subtract(BOX_HALF_WIDTH), request.longitude().add(BOX_HALF_WIDTH))
                .stream()
                .filter(place -> place.getStatus() == PlaceStatus.ACTIVE)
                .toList();
        return placeMatcher.findUniqueMatch(candidates, request.placeName(), request.address(),
                        request.latitude(), request.longitude())
                .map(Place::getPlaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
    }

    private void validate(ResolvePlaceRequest request) {
        if (request == null
                || isBlank(request.externalPlaceId()) || isBlank(request.placeName()) || isBlank(request.address())
                || request.latitude() == null || request.longitude() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (request.latitude().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || request.longitude().abs().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new BusinessException(ErrorCode.INVALID_COORDINATES);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
