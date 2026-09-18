package com.travelbird.trip.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TripTest {
  @Test
  void manualTripCreatesOneEmptyDayPerDate() {
    var trip = Trip.createManual(null, null, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 10),
        CompanionType.FRIENDS, Set.of(TravelTheme.FOOD), Pace.NORMAL, LocalDateTime.of(2026, 9, 1, 0, 0));

    assertThat(trip.getSourceType()).isEqualTo(TripSourceType.MANUAL);
    assertThat(trip.getVisibility()).isEqualTo(Visibility.PRIVATE);
    assertThat(trip.getDays()).extracting(TripDay::getDayNumber).containsExactly(1, 2, 3);
  }

  @Test
  void samePlaceCannotExistTwiceAcrossTripDays() {
    var trip = Trip.createManual(null, null, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9),
        CompanionType.SOLO, Set.of(TravelTheme.NATURE), Pace.RELAXED, LocalDateTime.of(2026, 9, 1, 0, 0));
    trip.addPlace(1, 44L);

    assertThatThrownBy(() -> trip.addPlace(2, 44L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.PLACE_ALREADY_ADDED);
  }

  @Test
  void cancelledTripRejectsEveryMutation() {
    var trip = Trip.createManual(null, null, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8),
        CompanionType.SOLO, Set.of(TravelTheme.NATURE), Pace.RELAXED, LocalDateTime.of(2026, 9, 1, 0, 0));
    trip.cancel(LocalDateTime.of(2026, 9, 2, 0, 0));

    assertThatThrownBy(() -> trip.addWishlistPlace(1L))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.TRIP_CANCELLED_READ_ONLY);
  }
  @Test
  void sixteenthPlaceInOneDayIsRejected() {
    var trip = Trip.createManual(null, null, LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8), CompanionType.SOLO, Set.of(TravelTheme.NATURE), Pace.NORMAL, LocalDateTime.now());
    for (long id = 1; id <= 15; id++) trip.addPlace(1, id);
    assertThatThrownBy(() -> trip.addPlace(1, 16L)).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_DAY_PLACE_LIMIT_EXCEEDED));
  }  @Test void themesOnlyPatchDoesNotRequireCompanionOrPaceValues() {
    var trip=Trip.createManual(null,null,LocalDate.now(),LocalDate.now(),CompanionType.SOLO,Set.of(TravelTheme.FOOD),Pace.NORMAL,LocalDateTime.now());
    trip.updateBasic(null,false,null,false,null,false,Set.of(TravelTheme.NATURE),true,null,false,null,false);
    assertThat(trip.getThemes()).containsExactly(TravelTheme.NATURE);
  }}
