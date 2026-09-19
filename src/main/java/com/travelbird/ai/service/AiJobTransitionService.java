package com.travelbird.ai.service;

import com.travelbird.ai.entity.*;
import com.travelbird.ai.repository.AiRecommendationJobRepository;
import com.travelbird.global.error.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AiJobTransitionService {
  private final AiRecommendationJobRepository jobs;
  private final RecommendationSnapshotCodec snapshots;
  private final RecommendationPayloadFingerprint fingerprints;
  private final Clock clock;

  public AiJobTransitionService(AiRecommendationJobRepository jobs,
      RecommendationSnapshotCodec snapshots, RecommendationPayloadFingerprint fingerprints,
      Clock clock) {
    this.jobs = jobs; this.snapshots = snapshots; this.fingerprints = fingerprints; this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean prepare(Long jobId, String suppliedFingerprint) {
    var job = get(jobId);
    String stored = fingerprints.sha256(snapshots.read(job));
    if (!stored.equals(suppliedFingerprint)) throw new BusinessException(ErrorCode.AI_JOB_ID_CONFLICT);
    return job.getStatus() == BackendAiJobStatus.QUEUED;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processing(Long jobId) {
    var job = get(jobId);
    if (job.getStatus() == BackendAiJobStatus.QUEUED) job.start(LocalDateTime.now(clock));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void failed(Long jobId, AiJobFailureCode code, String message, boolean retryable) {
    var job = get(jobId);
    if (job.getStatus() == BackendAiJobStatus.QUEUED || job.getStatus() == BackendAiJobStatus.PROCESSING) {
      job.fail(code, message, retryable, LocalDateTime.now(clock));
    }
  }

  private AiRecommendationJob get(Long id) {
    return jobs.findByIdForUpdate(id).orElseThrow(() -> new BusinessException(ErrorCode.AI_JOB_NOT_FOUND));
  }
}
