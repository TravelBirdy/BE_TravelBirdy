package com.travelbird.trip.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import java.util.List;
import java.util.Objects;

public final class TripImagePolicy {
  private TripImagePolicy() {}

  public static void validateFileIds(List<Long> fileIds) {
    if (fileIds == null || fileIds.stream().anyMatch(Objects::isNull)) {
      throw new BusinessException(ErrorCode.INVALID_REQUEST);
    }
  }
}
