package com.travelbird.ai.service;

import com.travelbird.ai.api.AiPreviewDisplayReader;
import com.travelbird.ai.repository.AiTripPreviewRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPreviewDisplayReaderImpl implements AiPreviewDisplayReader {
    private final AiTripPreviewRepository previews;
    private final RecommendationSnapshotCodec snapshots;
    private final Clock clock;
    public AiPreviewDisplayReaderImpl(AiTripPreviewRepository previews,
            RecommendationSnapshotCodec snapshots, Clock clock) {
        this.previews=previews; this.snapshots=snapshots; this.clock=clock;
    }
    @Override
    @Transactional(readOnly=true)
    public Optional<Summary> findOwned(Long userId, Long previewId) {
        return previews.findById(previewId)
            .filter(preview -> preview.ownedBy(userId) && preview.getRetentionStatus() == com.travelbird.ai.entity.AiPreviewRetentionStatus.PERMANENT && !preview.expired(LocalDateTime.now(clock)))
            .map(preview -> new Summary(preview.getTripTitle(), preview.getJob().getRegionCode(),
                preview.getDays().stream().flatMap(day -> day.getPlaces().stream())
                    .map(place -> place.getPlaceId()).toList(),
                snapshots.read(preview.getJob()).themes(), preview.getUpdatedAt()));
    }
}
