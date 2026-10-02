package com.travelbird.ai.api;

import com.travelbird.common.enums.TravelTheme;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Current owned Preview content for the existing SavedRoute response contract. */
public interface AiPreviewDisplayReader {
    Optional<Summary> findOwned(Long userId, Long previewId);
    record Summary(String title, String regionCode, List<Long> placeIds,
                   List<TravelTheme> themes, LocalDateTime updatedAt) {}
}
