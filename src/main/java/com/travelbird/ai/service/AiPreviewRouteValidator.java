package com.travelbird.ai.service;

import com.travelbird.ai.dto.internal.*;
import com.travelbird.global.error.AiJobFailureCode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class AiPreviewRouteValidator {
  public void validate(List<AiResultDay> days, LocalDate start, LocalDate end,
      Set<Long> allowed, Set<Long> required, boolean additional) {
    if (days == null) fail(AiJobFailureCode.AI_RESULT_VALIDATION_FAILED);
    int dayCount = (int) ChronoUnit.DAYS.between(start, end) + 1;
    Set<Long> allPlaces = new HashSet<>();
    Set<Integer> dayNumbers = new HashSet<>();
    for (AiResultDay day : days) {
      if (day == null || day.dayNumber() == null || day.dayNumber() < 1
          || day.dayNumber() > dayCount || !dayNumbers.add(day.dayNumber())
          || day.places() == null || day.places().size() > 15) {
        fail(AiJobFailureCode.AI_RESULT_VALIDATION_FAILED);
      }
      Set<Integer> orders = new HashSet<>();
      for (AiResultPlace place : day.places()) {
        if (place == null || place.placeId() == null) fail(AiJobFailureCode.AI_PLACE_ID_REQUIRED);
        if (place.order() == null || place.order() < 1 || !orders.add(place.order())) {
          fail(AiJobFailureCode.AI_PLACE_ORDER_INVALID);
        }
        if (!allPlaces.add(place.placeId())) fail(AiJobFailureCode.AI_RESULT_VALIDATION_FAILED);
        if (place.reason() == null || AiTextEscaper.escape(place.reason()).length() > 100 || place.reason().replaceAll("\\s", "").length() < 10
            || place.reason().replaceAll("\\s", "").length() > 100) {
          fail(AiJobFailureCode.AI_RECOMMENDATION_REASON_INVALID);
        }
      }
      for (int order = 1; order <= day.places().size(); order++) {
        if (!orders.contains(order)) fail(AiJobFailureCode.AI_PLACE_ORDER_INVALID);
      }
    }
    if (!allPlaces.containsAll(required) || (!additional && !allowed.containsAll(allPlaces))) {
      fail(AiJobFailureCode.AI_RESULT_VALIDATION_FAILED);
    }
  }

  private void fail(AiJobFailureCode code) { throw new AiResultValidationException(code); }
}