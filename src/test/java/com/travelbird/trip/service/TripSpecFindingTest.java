package com.travelbird.trip.service;
import com.travelbird.common.enums.*;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
import com.travelbird.file.entity.*; import com.travelbird.file.storage.ObjectStorage; import com.travelbird.global.error.*; import com.travelbird.place.entity.Place; import com.travelbird.trip.dto.request.*; import com.travelbird.trip.entity.*; import java.time.*; import java.util.*; import org.junit.jupiter.api.Test;
class TripSpecFindingTest {
 @Test void uploadedFileBecomesLinkedAndHasRenderableSummary(){var file=mock(FileMetadata.class);when(file.getId()).thenReturn(5L);when(file.objectKey()).thenReturn("uploads/5.webp");var storage=mock(ObjectStorage.class);when(storage.presignGet("uploads/5.webp",Duration.ofMinutes(10))).thenReturn(java.net.URI.create("https://images.test/5.webp?sig=x"));assertThat(TripImagePolicy.linkAndSummarize(file,storage).imageUrl()).startsWith("https://images.test/");verify(file).link();}
 @Test void createValidationUsesDedicatedCodes(){assertCode(()->Trip.createManual(null,null,null,LocalDate.now(),CompanionType.SOLO,Set.of(TravelTheme.FOOD),Pace.NORMAL,LocalDateTime.now()),ErrorCode.TRIP_DATE_REQUIRED);assertCode(()->Trip.createManual(null,null,LocalDate.now(),LocalDate.now(),null,Set.of(TravelTheme.FOOD),Pace.NORMAL,LocalDateTime.now()),ErrorCode.INVALID_COMPANION_TYPE);assertCode(()->Trip.createManual(null,null,LocalDate.now(),LocalDate.now(),CompanionType.SOLO,Set.of(TravelTheme.FOOD),null,LocalDateTime.now()),ErrorCode.INVALID_TRIP_PACE);}
 @Test void explicitNullConfirmationIsInvalid(){var r=new UpdateTripRequest();r.setConfirmDayRemoval(null);assertThat(r.hasInvalidRequiredFields()).isTrue();}
 @Test void cancelledListCategoryIsRejected(){assertThatThrownBy(()->TripListPolicy.validate(TripStatus.CANCELLED)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));}
 private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,ErrorCode code){assertThatThrownBy(call).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(code));}
}

