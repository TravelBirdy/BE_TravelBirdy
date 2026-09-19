package com.travelbird.place.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.domain.Place;
import com.travelbird.place.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaceReaderImpl implements PlaceReader {

    private final PlaceRepository placeRepository;
    private final SavedPlaceReader savedPlaceReader;

    @Override
    public boolean existsPlace(Long placeId) {
        return placeRepository.existsById(placeId);
    }

    @Override
    public PlaceContract getPlace(Long placeId, Long viewerIdOrNull) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
        boolean saved = viewerIdOrNull != null
                && savedPlaceReader.areAllSavedByUser(viewerIdOrNull, List.of(placeId));
        return toContract(place, saved);
    }

    @Override
    public List<PlaceContract> getPlaces(List<Long> placeIds, Long viewerIdOrNull) {
        Set<Long> savedPlaceIds = viewerIdOrNull == null
                ? Set.of()
                : Set.copyOf(savedPlaceReader.getSavedPlaceIds(viewerIdOrNull));
        return placeRepository.findAllByPlaceIdIn(placeIds).stream()
                .map(place -> toContract(place, savedPlaceIds.contains(place.getPlaceId())))
                .toList();
    }

    private static PlaceContract toContract(Place place, boolean saved) {
        return new PlaceContract(
                place.getPlaceId(),
                place.getName(),
                place.getAddress(),
                place.getSigunguCode(),
                place.getCategory(),
                place.getLatitude(),
                place.getLongitude(),
                place.getStatus(),
                saved
        );
    }
}
