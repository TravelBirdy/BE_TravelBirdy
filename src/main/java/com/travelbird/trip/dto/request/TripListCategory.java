package com.travelbird.trip.dto.request;

import com.travelbird.trip.entity.TripStatus;

public enum TripListCategory {
  UPCOMING,
  IN_PROGRESS,
  COMPLETED;

  public TripStatus toStatus() {
    return TripStatus.valueOf(name());
  }
}
