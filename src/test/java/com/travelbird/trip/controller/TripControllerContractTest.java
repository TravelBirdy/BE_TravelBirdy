package com.travelbird.trip.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.*;

class TripControllerContractTest {
 @Test void exposesAllTask8TripOperations(){
  assertThat(TripController.class.getAnnotation(RequestMapping.class).value()).containsExactly("/api");
  assertThat(methods(PostMapping.class)).contains("/trips","/trips/{tripId}/days/{dayNumber}/places","/trips/{tripId}/wishlist-places","/trips/{tripId}/cancel");
  assertThat(methods(GetMapping.class)).contains("/trips/{tripId}","/trips/{tripId}/wishlist-places","/users/me/trips","/users/me/travel-calendar","/trips/{tripId}/days/{dayNumber}/route");
  assertThat(methods(DeleteMapping.class)).contains("/trips/{tripId}/days/{dayNumber}/places/{tripPlaceId}","/trips/{tripId}/wishlist-places/{placeId}");
  assertThat(methods(PutMapping.class)).contains("/trips/{tripId}/days/{dayNumber}/place-orders");
  assertThat(methods(PatchMapping.class)).contains("/trips/{tripId}","/trips/{tripId}/places/{tripPlaceId}/content");
 }
 private <A extends java.lang.annotation.Annotation> java.util.List<String> methods(Class<A> type){return java.util.Arrays.stream(TripController.class.getDeclaredMethods()).map(m->m.getAnnotation(type)).filter(java.util.Objects::nonNull).map(a->{try{return ((String[])type.getMethod("value").invoke(a))[0];}catch(Exception e){throw new RuntimeException(e);}}).toList();}
}
