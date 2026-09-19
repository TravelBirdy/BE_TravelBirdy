package com.travelbird.ai.service;

import com.travelbird.ai.api.AiPreviewWithdrawalCleanup;
import com.travelbird.ai.repository.AiTripPreviewRepository;
import com.travelbird.savedroute.api.SavedRouteReferenceReader;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiPreviewWithdrawalCleanupService implements AiPreviewWithdrawalCleanup {
  private final AiTripPreviewRepository previews;
  private final SavedRouteReferenceReader references;
  private final Clock clock;
  public AiPreviewWithdrawalCleanupService(
      AiTripPreviewRepository previews, SavedRouteReferenceReader references, Clock clock) {
    this.previews = previews;
    this.references = references;
    this.clock = clock;
  }
  @Override
  @Transactional
  public void demoteUnreferencedPreviews(Collection<Long> previewIds) {
    if (previewIds == null || previewIds.isEmpty()) return;
    var now = LocalDateTime.now(clock);
    for (Long previewId : new LinkedHashSet<>(previewIds)) {
      if (previewId == null || references.existsAvailableAiPreviewReference(previewId)) continue;
      previews.findByIdForGate(previewId).ifPresent(preview -> preview.makeTemporary(now));
    }
  }
}
