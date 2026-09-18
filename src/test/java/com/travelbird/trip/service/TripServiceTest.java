package com.travelbird.trip.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.travelbird.global.error.*;
import com.travelbird.place.repository.PlaceRepository;
import com.travelbird.region.repository.SigunguMasterRepository;
import com.travelbird.trip.dto.request.*;
import com.travelbird.trip.entity.*;
import com.travelbird.trip.repository.*;
import com.travelbird.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class TripServiceTest {
  TripRepository trips=mock(TripRepository.class);
  TripWishlistRepository wishlist=mock(TripWishlistRepository.class);
  PlaceRepository places=mock(PlaceRepository.class);
  UserRepository users=mock(UserRepository.class);
  SigunguMasterRepository regions=mock(SigunguMasterRepository.class);
  TripService service=new TripService(trips,wishlist,places,users,regions,Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"),ZoneId.of("Asia/Seoul")));

  @Test void routeMutationIsRejectedWhenPublishedPostExists(){
    Trip trip=mock(Trip.class); when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip)); when(trip.ownedBy(7L)).thenReturn(true); when(trips.existsPublishedPost(1L)).thenReturn(true);
    assertThatThrownBy(()->service.addPlace(7L,1L,1,new AddTripPlaceRequest(9L,null)))
        .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
  }

  @Test void addPlaceRequiresExistingCanonicalPlace(){
    Trip trip=mock(Trip.class); when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip)); when(trip.ownedBy(7L)).thenReturn(true);
    assertThatThrownBy(()->service.addPlace(7L,1L,1,new AddTripPlaceRequest(9L,null)))
        .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(ErrorCode.PLACE_NOT_FOUND));
  }
  @Test void wishlistMutationLocksTripAggregateBeforeCheckingExclusivity(){
    Trip trip=mock(Trip.class); when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip)); when(trip.ownedBy(7L)).thenReturn(true); var place=mock(com.travelbird.place.entity.Place.class); when(places.findById(9L)).thenReturn(Optional.of(place));
    service.addWishlist(7L,1L,new WishlistPlaceRequest(9L));
    verify(trips).findByIdForUpdate(1L); verify(trip).addWishlistPlace(9L);
  }

  @Test void cancelRejectionIncludesPublishedPostIdInDetails(){
    Trip trip=mock(Trip.class);
    when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
    when(trip.ownedBy(7L)).thenReturn(true);
    when(trips.findPublishedPostId(1L)).thenReturn(Optional.of(42L));
    assertThatThrownBy(()->service.cancel(7L,1L))
        .isInstanceOfSatisfying(BusinessException.class,e->{
          assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_CANCEL_REQUIRES_POST_DELETION);
          assertThat(e.details()).containsEntry("postId",42L);
        });
  }
}
