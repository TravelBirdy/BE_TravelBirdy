package com.travelbird.trip.service;

import static org.assertj.core.api.Assertions.*;

import com.travelbird.common.enums.*;
import com.travelbird.global.error.*;
import com.travelbird.trip.dto.request.UpdateTripRequest;
import com.travelbird.trip.entity.*;
import java.time.*;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TripSpecFindingTest {

  @Test
  void createValidationUsesDedicatedCodes() {
    assertCode(
        () -> Trip.createManual(
            null,
            null,
            null,
            LocalDate.now(),
            CompanionType.SOLO,
            Set.of(TravelTheme.FOOD),
            Pace.NORMAL,
            LocalDateTime.now()),
        ErrorCode.TRIP_DATE_REQUIRED);
    assertCode(
        () -> Trip.createManual(
            null,
            null,
            LocalDate.now(),
            LocalDate.now(),
            null,
            Set.of(TravelTheme.FOOD),
            Pace.NORMAL,
            LocalDateTime.now()),
        ErrorCode.INVALID_COMPANION_TYPE);
    assertCode(
        () -> Trip.createManual(
            null,
            null,
            LocalDate.now(),
            LocalDate.now(),
            CompanionType.SOLO,
            Set.of(TravelTheme.FOOD),
            null,
            LocalDateTime.now()),
        ErrorCode.INVALID_TRIP_PACE);
  }

  @Test
  void explicitNullConfirmationIsInvalid() {
    var request = new UpdateTripRequest();
    request.setConfirmDayRemoval(null);
    assertThat(request.hasInvalidRequiredFields()).isTrue();
  }

  @Test
  void cancelledListCategoryIsRejected() {
    assertThatThrownBy(() -> TripListPolicy.validate(TripStatus.CANCELLED))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  private void assertCode(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode code) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(code));
  }
}
