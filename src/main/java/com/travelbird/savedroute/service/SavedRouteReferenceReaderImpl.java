package com.travelbird.savedroute.service;

import com.travelbird.savedroute.api.SavedRouteReferenceReader;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedRouteReferenceReaderImpl implements SavedRouteReferenceReader {

    private final SavedRouteRepository savedRouteRepository;

    @Override
    public boolean existsAvailableAiPreviewReference(Long previewId) {
        return savedRouteRepository.existsBySourceTypeAndSourceIdAndSourceAvailableTrue(
                SavedRouteSourceType.AI_PREVIEW, previewId);
    }
}
