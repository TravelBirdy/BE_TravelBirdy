package com.travelbird.trip.api;

/** Part3가 Post 작성 전에 Trip 존재 여부와 소유권을 검증할 때 사용하는 Part2 계약. */
public interface TripPostReader {

  /**
   * Trip이 없거나 사용자가 소유하지 않으면 Part2의 업무 예외를 발생시킨다.
   */
  void validateOwnedTrip(Long userId, Long tripId);
}
