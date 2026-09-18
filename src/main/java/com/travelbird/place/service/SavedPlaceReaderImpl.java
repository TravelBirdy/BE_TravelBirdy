package com.travelbird.place.service;

import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.place.repository.SavedPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SavedPlaceReaderImpl implements SavedPlaceReader {

    private final SavedPlaceRepository savedPlaceRepository;

    @Override
    public boolean areAllSavedByUser(Long userId, List<Long> placeIds) {
        Set<Long> distinctPlaceIds = new HashSet<>(placeIds);
        if (distinctPlaceIds.isEmpty()) {
            return true;
        }
        long savedCount = savedPlaceRepository.countByIdUserIdAndIdPlaceIdIn(userId, distinctPlaceIds);
        return savedCount == distinctPlaceIds.size();
    }

    @Override
    public List<Long> getSavedPlaceIds(Long userId) {
        return savedPlaceRepository.findPlaceIdsByUserId(userId);
    }
}
