package com.travelbird.trip.controller;

import com.travelbird.auth.security.AuthenticatedUser;
import com.travelbird.trip.dto.request.*;
import com.travelbird.trip.dto.response.*;
import com.travelbird.trip.entity.TripStatus;
import com.travelbird.trip.service.TripService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
public class TripController {
 private final TripService service; public TripController(TripService service){this.service=service;}
 @PostMapping("/trips") public ResponseEntity<CreateTripResponse> create(@AuthenticationPrincipal AuthenticatedUser u,@Valid @RequestBody CreateTripRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.createResponse(u.userId(),r));}
 @GetMapping("/trips/{tripId}") public TripResponse detail(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId){return service.detail(id(u),tripId);}
 @PatchMapping("/trips/{tripId}") public TripResponse update(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@RequestBody UpdateTripRequest r){return service.update(u.userId(),tripId,r);}
 @PostMapping("/trips/{tripId}/days/{dayNumber}/places") public ResponseEntity<Void> add(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable int dayNumber,@Valid @RequestBody AddTripPlaceRequest r){service.addPlace(u.userId(),tripId,dayNumber,r);return ResponseEntity.noContent().build();}
 @DeleteMapping("/trips/{tripId}/days/{dayNumber}/places/{tripPlaceId}") public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable int dayNumber,@PathVariable Long tripPlaceId){service.removePlace(u.userId(),tripId,dayNumber,tripPlaceId);return ResponseEntity.noContent().build();}
 @PutMapping("/trips/{tripId}/days/{dayNumber}/place-orders") public ResponseEntity<Void> reorder(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable int dayNumber,@Valid @RequestBody ReorderTripPlacesRequest r){service.reorder(u.userId(),tripId,dayNumber,r);return ResponseEntity.noContent().build();}
 @PatchMapping("/trips/{tripId}/places/{tripPlaceId}/content") public ResponseEntity<Void> content(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable Long tripPlaceId,@RequestBody UpdateTripPlaceContentRequest r){service.updateContent(u.userId(),tripId,tripPlaceId,r);return ResponseEntity.noContent().build();}
 @PostMapping("/trips/{tripId}/wishlist-places") public ResponseEntity<Void> addWishlist(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@Valid @RequestBody WishlistPlaceRequest r){service.addWishlist(u.userId(),tripId,r);return ResponseEntity.noContent().build();}
 @GetMapping("/trips/{tripId}/wishlist-places") public WishlistPlaceListResponse wishlist(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId){return service.wishlist(u.userId(),tripId,null,null);}
 @DeleteMapping("/trips/{tripId}/wishlist-places/{placeId}") public ResponseEntity<Void> removeWishlist(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable Long placeId){service.removeWishlist(u.userId(),tripId,placeId);return ResponseEntity.noContent().build();}
 @GetMapping("/users/me/trips") public TripListResponse list(@AuthenticationPrincipal AuthenticatedUser u,@RequestParam TripListCategory category,@RequestParam(required=false) Long cursor,@RequestParam(required=false) Integer size){return service.list(u.userId(),category.toStatus(),cursor,size);}
 @GetMapping("/users/me/travel-calendar") public TravelCalendarResponse calendar(@AuthenticationPrincipal AuthenticatedUser u,@RequestParam LocalDate from,@RequestParam LocalDate to){return service.calendar(u.userId(),from,to);}
 @PostMapping("/trips/{tripId}/cancel") public ResponseEntity<Void> cancel(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId){service.cancel(u.userId(),tripId);return ResponseEntity.noContent().build();}
 @GetMapping("/trips/{tripId}/days/{dayNumber}/route") public RouteResponse route(@AuthenticationPrincipal AuthenticatedUser u,@PathVariable Long tripId,@PathVariable int dayNumber){return service.route(id(u),tripId,dayNumber);}
 private Long id(AuthenticatedUser u){return u==null?null:u.userId();}
}
