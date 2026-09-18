package com.travelbird.ai.service;

import com.travelbird.ai.dto.internal.*;
import com.travelbird.ai.entity.*;
import com.travelbird.ai.repository.*;
import com.travelbird.global.error.*;
import com.travelbird.place.repository.PlaceRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiCallbackService {
  private final AiRecommendationJobRepository jobs;
  private final AiTripPreviewRepository previews;
  private final AiPreviewPersistenceService persistence;
  private final AiPreviewRouteValidator validator;
  private final RecommendationSnapshotCodec snapshots;
  private final PlaceRepository places;
  private final Clock clock;

  public AiCallbackService(AiRecommendationJobRepository jobs, AiTripPreviewRepository previews,
      AiPreviewPersistenceService persistence, AiJobTransitionService ignored,
      AiPreviewRouteValidator validator, RecommendationSnapshotCodec snapshots,
      PlaceRepository places, Clock clock) {
    this.jobs=jobs; this.previews=previews; this.persistence=persistence;
    this.validator=validator; this.snapshots=snapshots; this.places=places; this.clock=clock;
  }

  @Transactional(noRollbackFor = AiResultValidationException.class)
  public AiCallbackResponse handle(AiCallbackRequest request) {
    if (request == null || request.jobId() == null || request.status() == null) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
    AiRecommendationJob job = jobs.findByIdForUpdate(request.jobId())
        .orElseThrow(() -> new BusinessException(ErrorCode.AI_JOB_NOT_FOUND));
    OffsetDateTime processedAt = OffsetDateTime.now(clock);
    if (isTerminal(job)) {
      Long previewId = previews.findByJobId(job.getId()).map(AiTripPreview::getId).orElse(null);
      return new AiCallbackResponse(true, job.getId(), previewId, processedAt);
    }
    if ("FAILED".equals(request.status())) {
      if (request.code() == null || request.message() == null || request.timestamp() == null) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST);
      }
      job.fail(request.code(), request.message(), false, LocalDateTime.now(clock));
      return new AiCallbackResponse(true, job.getId(), null, processedAt);
    }
    if (!"COMPLETED".equals(request.status())) throw new BusinessException(ErrorCode.INVALID_REQUEST);
    if (request.tripTitle() == null || request.summary() == null || request.days() == null) {
      failValidation(job, AiJobFailureCode.AI_RESULT_VALIDATION_FAILED,
          "AI callback result structure invalid");
    }
    var snapshot = snapshots.read(job);
    Set<Long> existing = new HashSet<>();
    snapshot.existingSchedule().forEach(day -> existing.addAll(day.placeIds()));
    Set<Long> required = new HashSet<>(existing); required.addAll(snapshot.wishlistPlaceIds());
    Set<Long> allowed = new HashSet<>(required); allowed.addAll(snapshot.savedPlaceIds());
    try {
      validator.validate(request.days(), snapshot.startDate(), snapshot.endDate(), allowed, required,
          snapshot.allowAdditionalRecommendations());
      Set<Long> ids = request.days().stream().flatMap(day -> day.places().stream())
          .map(AiResultPlace::placeId).collect(java.util.stream.Collectors.toSet());
      var found = places.findAllById(ids);
      if (found.size() != ids.size()) failValidation(job, AiJobFailureCode.AI_PLACE_NOT_FOUND,
          "AI callback contains an unknown place");
      if (found.stream().anyMatch(place -> !place.getRegion().getSigunguCode().equals(snapshot.regionCode()))) {
        failValidation(job, AiJobFailureCode.PLACE_REGION_MISMATCH,
            "AI callback contains a place outside the requested region");
      }
    } catch (AiResultValidationException exception) {
      if (job.getStatus() == BackendAiJobStatus.PROCESSING || job.getStatus() == BackendAiJobStatus.QUEUED) {
        job.fail(exception.failureCode(), "AI callback result validation failed", false, LocalDateTime.now(clock));
      }
      throw exception;
    }
    Long previewId = persistence.persist(job.getId(), request);
    return new AiCallbackResponse(true, job.getId(), previewId, processedAt);
  }

  private boolean isTerminal(AiRecommendationJob job) {
    return job.getStatus() == BackendAiJobStatus.SUCCEEDED
        || job.getStatus() == BackendAiJobStatus.FAILED
        || job.getStatus() == BackendAiJobStatus.EXPIRED;
  }

  private void failValidation(AiRecommendationJob job, AiJobFailureCode code, String message) {
    job.fail(code, message, false, LocalDateTime.now(clock));
    throw new AiResultValidationException(code);
  }
}