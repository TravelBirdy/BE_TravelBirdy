package com.travelbird.trip.api;

/** Part 2가 Part 1 회원탈퇴 Orchestrator에 제공하는 소유 데이터 정리 계약. */
public interface TripWithdrawalCleanup {
  void cleanupUserTrips(Long userId);
}
