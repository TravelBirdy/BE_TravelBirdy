package com.travelbird.trip.service;
import com.travelbird.common.enums.*;
import static org.assertj.core.api.Assertions.*; import com.travelbird.global.error.*; import com.travelbird.trip.dto.request.CreateTripRequest; import com.travelbird.trip.entity.*; import java.time.*; import java.util.List; import org.junit.jupiter.api.Test;
class TripThemeValidationTest {@Test void duplicateCreateThemesAreRejectedBeforeSetConversion(){var r=new CreateTripRequest("11110",LocalDate.now(),LocalDate.now(),CompanionType.SOLO,List.of(TravelTheme.FOOD,TravelTheme.FOOD),Pace.NORMAL);assertThatThrownBy(()->TripRequestPolicy.validatedThemes(r.themes())).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));}}

