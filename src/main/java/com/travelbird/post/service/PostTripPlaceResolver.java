package com.travelbird.post.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.trip.api.TripPostReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 요청의 {@code placeIds}(Part3 {@code placeId} 기준)를 해당 Trip 스냅샷의
 * {@code tripPlaceId}로 변환한다. {@code PostCreateService}/{@code PostUpdateService}가
 * 공유하는 로직이라 추출했다 — 트립 소유가 아닌 장소는 {@code 400 POST_PLACE_NOT_IN_TRIP}
 * (스펙에 전용 코드가 없어 신규 추가, PR#19 리뷰 요청 항목).
 */
@Component
public class PostTripPlaceResolver {

    public List<Long> resolve(TripPostReader.TripPostSnapshot trip, List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> tripPlaceIdByPlaceId = new HashMap<>();
        trip.days().forEach(day -> day.places().forEach(
                place -> tripPlaceIdByPlaceId.put(place.placeId(), place.tripPlaceId())));

        List<Long> tripPlaceIds = new ArrayList<>();
        for (Long placeId : placeIds) {
            Long tripPlaceId = tripPlaceIdByPlaceId.get(placeId);
            if (tripPlaceId == null) {
                throw new BusinessException(ErrorCode.POST_PLACE_NOT_IN_TRIP);
            }
            tripPlaceIds.add(tripPlaceId);
        }
        return tripPlaceIds;
    }
}
