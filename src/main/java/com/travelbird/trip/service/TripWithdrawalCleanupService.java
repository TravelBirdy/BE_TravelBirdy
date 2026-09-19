package com.travelbird.trip.service;

import com.travelbird.ai.repository.AiRecommendationJobRepository;
import com.travelbird.ai.repository.AiTripPreviewRepository;
import com.travelbird.trip.api.TripWithdrawalCleanup;
import com.travelbird.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripWithdrawalCleanupService implements TripWithdrawalCleanup {
  private final AiTripPreviewRepository previews;
  private final AiRecommendationJobRepository jobs;
  private final TripRepository trips;

  public TripWithdrawalCleanupService(
      AiTripPreviewRepository previews,
      AiRecommendationJobRepository jobs,
      TripRepository trips) {
    this.previews = previews;
    this.jobs = jobs;
    this.trips = trips;
  }

  @Override
  @Transactional
  public void cleanupUserTrips(Long userId) {
    previews.deleteAll(previews.findAllByUserId(userId));
    jobs.deleteAll(jobs.findAllByUserId(userId));
    trips.deleteAll(trips.findAllByUserId(userId));
  }
}
