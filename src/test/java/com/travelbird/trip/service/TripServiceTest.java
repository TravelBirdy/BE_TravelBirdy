package com.travelbird.trip.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.*;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.post.api.PostRouteLock;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.dto.request.*;
import com.travelbird.trip.entity.*;
import com.travelbird.trip.repository.*;
import com.travelbird.user.api.UserReader;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class TripServiceTest {
  TripRepository trips = mock(TripRepository.class);
  TripWishlistRepository wishlist = mock(TripWishlistRepository.class);
  PlaceReader places = mock(PlaceReader.class);
  UserReader users = mock(UserReader.class);
  RegionReader regions = mock(RegionReader.class);
  FileLinkService files = mock(FileLinkService.class);
  PostRouteLockReader routeLocks = mock(PostRouteLockReader.class);
  TripService service = new TripService(
      trips, wishlist, places, users, regions, files, routeLocks,
      Clock.fixed(Instant.parse("2026-09-08T00:00:00Z"), ZoneId.of("Asia/Seoul")));

  @Test
  void routeMutationIsRejectedWhenPublishedPostExists() {
    Trip trip = mock(Trip.class);
    when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
    when(trip.ownedBy(7L)).thenReturn(true);
    when(routeLocks.findActivePublishedPostByTripId(1L)).thenReturn(new PostRouteLock(42L, true));

    assertThatThrownBy(() -> service.addPlace(7L, 1L, 1, new AddTripPlaceRequest(9L, null)))
        .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.errorCode())
            .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
  }

  @Test
  void addPlaceRequiresExistingCanonicalPlace() {
    Trip trip = mock(Trip.class);
    when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
    when(trip.ownedBy(7L)).thenReturn(true);

    assertThatThrownBy(() -> service.addPlace(7L, 1L, 1, new AddTripPlaceRequest(9L, null)))
        .isInstanceOfSatisfying(BusinessException.class, error -> assertThat(error.errorCode())
            .isEqualTo(ErrorCode.PLACE_NOT_FOUND));
  }

  @Test
  void wishlistMutationLocksTripAggregateBeforeCheckingExclusivity() {
    Trip trip = mock(Trip.class);
    when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
    when(trip.ownedBy(7L)).thenReturn(true);
    when(places.existsPlace(9L)).thenReturn(true);

    service.addWishlist(7L, 1L, new WishlistPlaceRequest(9L));

    verify(trips).findByIdForUpdate(1L);
    verify(trip).addWishlistPlace(9L);
  }

  @Test
  void cancelRejectionIncludesPublishedPostIdInDetails() {
    Trip trip = mock(Trip.class);
    when(trips.findByIdForUpdate(1L)).thenReturn(Optional.of(trip));
    when(trip.ownedBy(7L)).thenReturn(true);
    when(routeLocks.findActivePublishedPostByTripId(1L)).thenReturn(new PostRouteLock(42L, true));

    assertThatThrownBy(() -> service.cancel(7L, 1L))
        .isInstanceOfSatisfying(BusinessException.class, error -> {
          assertThat(error.errorCode()).isEqualTo(ErrorCode.TRIP_CANCEL_REQUIRES_POST_DELETION);
          assertThat(error.details()).containsEntry("postId", 42L);
        });
  }
}
