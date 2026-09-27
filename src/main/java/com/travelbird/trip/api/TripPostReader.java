package com.travelbird.trip.api;
import com.travelbird.common.enums.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Part 3이 Post 작성·상세 처리에 사용하는 Part 2 읽기 계약. */
public interface TripPostReader {

  TripPostSnapshot getOwnedTripForPost(Long userId, Long tripId);

  /**
   * Locks the Trip for first publication inside the caller's existing write transaction.
   * The caller must persist the Post in that same transaction. Ordinary DRAFT saves use
   * getOwnedTripForPost instead. Cancellation validation remains the caller's policy.
   */
  TripPostSnapshot lockOwnedTripForPost(Long userId, Long tripId);

  TripPostSnapshot getTripForPost(Long tripId);

  List<TripPostPlaceSnapshot> getTripPlaceSnapshot(Long tripId);

  void validateTripOwnership(Long userId, Long tripId);

  List<Long> getTripIdsByUser(Long userId);

  /** 기존 소비자 호환용 별칭. */
  default void validateOwnedTrip(Long userId, Long tripId) {
    validateTripOwnership(userId, tripId);
  }

  record TripPostSnapshot(
      Long tripId,
      Long ownerUserId,
      String regionCode,
      String companionType,
      Set<String> themes,
      String visibility,
      LocalDateTime cancelledAt,
      List<TripPostDaySnapshot> days) {}

  record TripPostDaySnapshot(int dayNumber, List<TripPostPlaceSnapshot> places) {}

  record TripPostPlaceSnapshot(
      Long tripPlaceId,
      Long placeId,
      int dayNumber,
      int visitOrder,
      String memo,
      List<Long> imageFileIds) {}
}


