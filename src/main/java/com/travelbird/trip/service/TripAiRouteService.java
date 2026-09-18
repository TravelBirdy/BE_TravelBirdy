package com.travelbird.trip.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.ai.entity.AiTripPreview;
import com.travelbird.ai.service.RecommendationSnapshotCodec;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.entity.Trip;
import com.travelbird.trip.repository.TripPlaceRepository;
import com.travelbird.trip.repository.TripRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import org.springframework.stereotype.Service;

@Service
public class TripAiRouteService {
  private final TripRepository trips;
  private final TripPlaceRepository tripPlaces;
  private final RegionReader regions;
  private final PostRouteLockReader routeLocks;
  private final Clock clock;

  public TripAiRouteService(
      TripRepository trips,
      TripPlaceRepository tripPlaces,
      RegionReader regions,
      PostRouteLockReader routeLocks,
      Clock clock) {
    this.trips = trips;
    this.tripPlaces = tripPlaces;
    this.regions = regions;
    this.routeLocks = routeLocks;
    this.clock = clock;
  }

  public record Result(Trip trip, String type, LocalDateTime at) {}

  public Result apply(AiTripPreview preview, Long userId) {
    var job = preview.getJob();
    var now = LocalDateTime.now(clock);
    Trip trip;
    String type;
    if (job.getTargetTripId() == null) {
      if (!regions.existsBySigunguCode(job.getRegionCode())) {
        throw new BusinessException(ErrorCode.REGION_REQUIRED);
      }
      trip = Trip.createManual(
          job.getUserId(), job.getRegionCode(), job.getStartDate(), job.getEndDate(),
          job.getCompanionType(),
          new LinkedHashSet<>(new RecommendationSnapshotCodec(new ObjectMapper()).read(job).themes()),
          job.getPace(), now);
      trip.updateBasic(
          preview.getTripTitle(), true, preview.getSummary(), true,
          null, false, null, false, null, false, null, false);
      trip.markAiSource();
      type = "CREATED_NEW_TRIP";
    } else {
      trip = trips.findByIdForUpdate(job.getTargetTripId())
          .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
      if (!trip.ownedBy(userId)) throw new BusinessException(ErrorCode.TRIP_ACCESS_DENIED);
      trip.ensureMutable();
      var lock = routeLocks.findActivePublishedPostByTripId(trip.getId());
      if (lock != null && lock.routeLocked()) {
        throw new BusinessException(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST);
      }
      for (var day : trip.getDays()) tripPlaces.stageOrders(trip.getId(), day.getDayNumber());
      type = "UPDATED_EXISTING_TRIP";
    }
    trip.applyAiPreview(preview.getDays());
    return new Result(trips.save(trip), type, now);
  }
}
