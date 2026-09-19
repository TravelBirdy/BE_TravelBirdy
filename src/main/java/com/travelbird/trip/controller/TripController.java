package com.travelbird.trip.controller;


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
 @PostMapping("/trips") public ResponseEntity<CreateTripResponse> create(@AuthenticationPrincipal Long userId,@Valid @RequestBody CreateTripRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(service.createResponse(userId,r));}
 @GetMapping("/trips/{tripId}") public TripResponse detail(@AuthenticationPrincipal Long userId,@PathVariable Long tripId){return service.detail(userId,tripId);}
 @PatchMapping("/trips/{tripId}") public TripResponse update(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@RequestBody UpdateTripRequest r){return service.update(userId,tripId,r);}
 @PostMapping("/trips/{tripId}/days/{dayNumber}/places") public ResponseEntity<Void> add(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable int dayNumber,@Valid @RequestBody AddTripPlaceRequest r){service.addPlace(userId,tripId,dayNumber,r);return ResponseEntity.noContent().build();}
 @DeleteMapping("/trips/{tripId}/days/{dayNumber}/places/{tripPlaceId}") public ResponseEntity<Void> remove(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable int dayNumber,@PathVariable Long tripPlaceId){service.removePlace(userId,tripId,dayNumber,tripPlaceId);return ResponseEntity.noContent().build();}
 @PutMapping("/trips/{tripId}/days/{dayNumber}/place-orders") public ResponseEntity<Void> reorder(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable int dayNumber,@Valid @RequestBody ReorderTripPlacesRequest r){service.reorder(userId,tripId,dayNumber,r);return ResponseEntity.noContent().build();}
 @PatchMapping("/trips/{tripId}/places/{tripPlaceId}/content") public ResponseEntity<Void> content(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable Long tripPlaceId,@RequestBody UpdateTripPlaceContentRequest r){service.updateContent(userId,tripId,tripPlaceId,r);return ResponseEntity.noContent().build();}
 @PostMapping("/trips/{tripId}/wishlist-places") public ResponseEntity<Void> addWishlist(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@Valid @RequestBody WishlistPlaceRequest r){service.addWishlist(userId,tripId,r);return ResponseEntity.noContent().build();}
 @GetMapping("/trips/{tripId}/wishlist-places") public WishlistPlaceListResponse wishlist(@AuthenticationPrincipal Long userId,@PathVariable Long tripId){return service.wishlist(userId,tripId,null,null);}
 @DeleteMapping("/trips/{tripId}/wishlist-places/{placeId}") public ResponseEntity<Void> removeWishlist(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable Long placeId){service.removeWishlist(userId,tripId,placeId);return ResponseEntity.noContent().build();}
 @GetMapping("/users/me/trips") public TripListResponse list(@AuthenticationPrincipal Long userId,@RequestParam TripListCategory category,@RequestParam(required=false) Long cursor,@RequestParam(required=false) Integer size){return service.list(userId,category.toStatus(),cursor,size);}
 @GetMapping("/users/me/travel-calendar") public TravelCalendarResponse calendar(@AuthenticationPrincipal Long userId,@RequestParam LocalDate from,@RequestParam LocalDate to){return service.calendar(userId,from,to);}
 @PostMapping("/trips/{tripId}/cancel") public ResponseEntity<Void> cancel(@AuthenticationPrincipal Long userId,@PathVariable Long tripId){service.cancel(userId,tripId);return ResponseEntity.noContent().build();}
 @GetMapping("/trips/{tripId}/days/{dayNumber}/route") public RouteResponse route(@AuthenticationPrincipal Long userId,@PathVariable Long tripId,@PathVariable int dayNumber){return service.route(userId,tripId,dayNumber);}

}


