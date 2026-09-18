package com.travelbird.trip.service;
import static org.assertj.core.api.Assertions.*; import com.travelbird.global.error.*; import com.travelbird.trip.dto.request.UpdateTripRequest; import com.travelbird.trip.entity.TravelTheme; import java.util.*; import org.junit.jupiter.api.Test;
class TripResidualValidationTest {
 @Test void nullThemeElementIsInvalid(){assertCode(()->TripRequestPolicy.validatedThemes(Arrays.asList(TravelTheme.FOOD,null)),ErrorCode.INVALID_REQUEST);}
 @Test void explicitNullPatchThemesAreInvalidButEmptyThemesUseThemeRequired(){assertCode(()->TripRequestPolicy.validatedPatchThemes(null),ErrorCode.INVALID_REQUEST);assertCode(()->TripRequestPolicy.validatedThemes(List.of()),ErrorCode.THEME_REQUIRED);}
 @Test void nullImageIdIsRejectedBeforeLookup(){assertCode(()->TripImagePolicy.validateFileIds(Arrays.asList(1L,null)),ErrorCode.INVALID_REQUEST);}
 @Test void explicitNullPatchRegionIsInvalid(){var request=new UpdateTripRequest();request.setVersion(0L);request.setRegionCode(null);assertThat(request.hasInvalidRequiredFields()).isTrue();}
 private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable c,ErrorCode code){assertThatThrownBy(c).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(code));}
}
