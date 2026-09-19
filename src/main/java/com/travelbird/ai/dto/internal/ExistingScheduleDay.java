package com.travelbird.ai.dto.internal;

import java.util.List;

public record ExistingScheduleDay(int dayNumber, List<Long> placeIds) {
  public ExistingScheduleDay {
    placeIds = placeIds == null ? List.of() : List.copyOf(placeIds);
  }
}
