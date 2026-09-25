package com.travelbird.trip.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.trip.entity.Trip;
import com.travelbird.trip.repository.TripRepository;
import com.travelbird.trip.repository.TripPostSnapshotRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class TripPostReaderService implements TripPostReader {
  private final TripRepository trips;

  private final TripPostSnapshotRepository snapshots;
  public TripPostReaderService(TripRepository trips, TripPostSnapshotRepository snapshots) {
    this.trips = trips;
    this.snapshots = snapshots;
  }

  @Override
  @Transactional(readOnly = true)
  public TripPostSnapshot getOwnedTripForPost(Long userId, Long tripId) {
    Trip trip = requireTrip(tripId);
    validateOwner(trip, userId);
    return snapshot(trip);
  }

  @Override
  @Transactional(readOnly = true)
  public TripPostSnapshot getTripForPost(Long tripId) {
    return snapshot(requireTrip(tripId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<TripPostPlaceSnapshot> getTripPlaceSnapshot(Long tripId) {
    return requireTrip(tripId).getDays().stream()
        .flatMap(day -> day.getPlaces().stream().map(place -> placeSnapshot(day.getDayNumber(), place)))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public void validateTripOwnership(Long userId, Long tripId) {
    validateOwner(requireTrip(tripId), userId);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getTripIdsByUser(Long userId) {
    return trips.findAllByUserId(userId).stream()
        .map(Trip::getId)
        .toList();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public TripPostSnapshot lockOwnedTripForPost(Long userId, Long tripId) {
    TripPostSnapshot trip = snapshots.lockHeader(tripId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    if (!java.util.Objects.equals(trip.ownerUserId(), userId)) {
      throw new BusinessException(ErrorCode.TRIP_ACCESS_DENIED);
    }
    return snapshots.readCurrentContents(trip);
  }

  private Trip requireTrip(Long tripId) {
    return trips.findById(tripId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
  }

  private void validateOwner(Trip trip, Long userId) {
    if (!trip.ownedBy(userId)) throw new BusinessException(ErrorCode.TRIP_ACCESS_DENIED);
  }

  private TripPostSnapshot snapshot(Trip trip) {
    var days = trip.getDays().stream()
        .map(day -> new TripPostDaySnapshot(
            day.getDayNumber(),
            day.getPlaces().stream().map(place -> placeSnapshot(day.getDayNumber(), place)).toList()))
        .toList();
    return new TripPostSnapshot(
        trip.getId(), trip.getUserId(), trip.getRegionCode(), trip.getCompanionType().name(),
        trip.getThemes().stream().map(Enum::name).collect(Collectors.toUnmodifiableSet()),
        trip.getVisibility().name(), trip.getCancelledAt(), days);
  }

  private TripPostPlaceSnapshot placeSnapshot(int dayNumber, com.travelbird.trip.entity.TripPlace place) {
    return new TripPostPlaceSnapshot(
        place.getId(), place.getPlaceId(), dayNumber, place.getVisitOrder(), place.getMemo(),
        place.getImages().stream().map(com.travelbird.trip.entity.TripPlaceImage::getFileId).toList());
  }
}
