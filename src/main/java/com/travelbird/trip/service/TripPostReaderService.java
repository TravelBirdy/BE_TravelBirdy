package com.travelbird.trip.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.trip.repository.TripRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripPostReaderService implements TripPostReader {

  private final TripRepository trips;

  public TripPostReaderService(TripRepository trips) {
    this.trips = trips;
  }

  @Override
  @Transactional(readOnly = true)
  public void validateOwnedTrip(Long userId, Long tripId) {
    var trip = trips.findById(tripId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    if (!trip.ownedBy(userId)) {
      throw new BusinessException(ErrorCode.TRIP_ACCESS_DENIED);
    }
  }
}
